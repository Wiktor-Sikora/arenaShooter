package io.github.arenaShooter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkClient {
    public interface MessageListener {
        void onMessage(String message);
    }

    private static final int MAX_PACKET_SIZE = 4096;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final String clientId = UUID.randomUUID().toString();

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int playerId = -1;
    private Thread listenThread;

    public boolean connect(String host, int port) throws IOException {
        return connect(host, port, 2000);
    }

    public synchronized boolean connect(String host, int port, int timeoutMillis) throws IOException {
        if (connected.get()) {
            return true;
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be in range 1-65535");
        }

        serverAddress = resolveHost(host.trim());
        serverPort = port;

        socket = new DatagramSocket();
        socket.connect(serverAddress, serverPort);
        socket.setSoTimeout(timeoutMillis);

        try {
            sendRaw("CONNECT " + clientId);
            String response = receiveRaw();
            if (!response.startsWith("ACCEPT")) {
                disconnect();
                return false;
            }
            String[] parts = response.split("\\s+");
            if (parts.length >= 2) {
                try {
                    playerId = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    playerId = -1;
                }
            }
        } catch (SocketTimeoutException timeout) {
            disconnect();
            return false;
        }

        socket.setSoTimeout(0);
        connected.set(true);
        startListenThread();
        return true;
    }

    public synchronized void disconnect() {
        listening.set(false);
        connected.set(false);

        if (listenThread != null) {
            listenThread.interrupt();
            listenThread = null;
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        socket = null;
        serverAddress = null;
        serverPort = 0;
        playerId = -1;
    }

    public void sendMessage(String message) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("Client is not connected");
        }
        if (message == null) {
            return;
        }
        sendRaw(message);
    }

    public boolean isConnected() {
        return connected.get();
    }

    public String getServerIp() {
        return serverAddress == null ? "" : serverAddress.getHostAddress();
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getClientId() {
        return clientId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public static List<String> getLocalIpv4Addresses() {
        List<String> addresses = new ArrayList<>();

        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        addresses.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            // Return whatever has already been collected.
        }

        return addresses;
    }

    private InetAddress resolveHost(String host) throws IOException {
        if ("localhost".equalsIgnoreCase(host)) {
            return InetAddress.getByName("127.0.0.1");
        }

        if ("0.0.0.0".equals(host)) {
            return InetAddress.getByName("127.0.0.1");
        }

        return InetAddress.getByName(host);
    }

    private void startListenThread() {
        listening.set(true);
        listenThread = new Thread(() -> {
            while (listening.get() && socket != null && !socket.isClosed()) {
                try {
                    String message = receiveRaw();
                    if (message.isBlank()) {
                        continue;
                    }
                    for (MessageListener listener : listeners) {
                        listener.onMessage(message);
                    }
                } catch (SocketException e) {
                    listening.set(false);
                    connected.set(false);
                } catch (IOException ignored) {
                    // Keep listening when malformed packets are received.
                }
            }
        }, "network-client-listener");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private String receiveRaw() throws IOException {
        if (socket == null) {
            throw new IllegalStateException("Socket not initialized");
        }

        byte[] buffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
    }

    private void sendRaw(String payload) throws IOException {
        if (socket == null || serverAddress == null) {
            throw new IllegalStateException("Socket not initialized");
        }

        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, serverAddress, serverPort);
        socket.send(packet);
    }
}
