package io.github.arenaShooter;

import io.github.arenaShooter.enemies.Enemy;
import io.github.arenaShooter.ui.ShopUI;
import io.github.arenaShooter.weapons.Bullet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkServer implements Runnable {
    public enum GameState {
        LOBBY,
        PLAYING,
        PAUSED,
        DEAD,
        STORE
    }

    private int globalGold = 0;

    public static class PlayerInput {
        public final int playerId;
        public final long tick;
        public final float moveX;
        public final float moveY;
        public final boolean fire;
        public final float rotation;
        public final String weaponName;
        public final float health;

        public PlayerInput(int playerId, long tick, float moveX, float moveY, boolean fire, float rotation, String weaponName, float health) {
            this.playerId = playerId;
            this.tick = tick;
            this.moveX = moveX;
            this.moveY = moveY;
            this.fire = fire;
            this.rotation = rotation;
            this.weaponName = weaponName;
            this.health = health;
        }
    }

    public GameState gameState = GameState.LOBBY;

    public CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Enemy> enemies = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();

    private final float MAP_TEXTURE_SIZE = 1500;
    public final float PLAYABLE_AREA_SIZE = 1400;
    public final float AREA_OFFSET = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;
    float WORLD_WIDTH = 1500f;
    float WORLD_HEIGHT = 1500f;

    private static final long TICK_RATE_HZ = 60L;
    private static final double TICK_DT_SECONDS = 1.0 / TICK_RATE_HZ;
    private static final double MAX_FRAME_SECONDS = 0.25;
    private static final int MAX_TICKS_PER_FRAME = 5;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<PlayerInput> pendingInputs = new ConcurrentLinkedQueue<>();
    private final Map<Integer, Long> lastProcessedInputTick = new ConcurrentHashMap<>();
    private final Map<String, SocketAddress> connectedClients = new ConcurrentHashMap<>();
    private final Map<String, Integer> clientPlayerIds = new ConcurrentHashMap<>();
    private final Map<String, Boolean> clientReadyStates = new ConcurrentHashMap<>();
    private final Map<String, ClientState> clientStates = new ConcurrentHashMap<>();

    private volatile long serverTick = 0L;
    private DatagramSocket socket;
    private int nextPlayerId = 1;
    private String ownerClientId;

    private static class ClientState {
        int playerId;
        float x;
        float y;
        float hp;
        float rotation;
        String weaponName;
        int enemiesKilled;
        int goldEarned;
        int damageTaken;

        int speedBonus;
        int maxHpBonus;
        int damageBonus;
    }

    private static final int GOLD_PER_KILL = 30;

    public void enqueueInput(PlayerInput input) {
        if (input != null) {
            pendingInputs.offer(input);
        }
    }

    public long getServerTick() {
        return serverTick;
    }

    public synchronized void start(int port) throws SocketException {
        if (running.get()) {
            return;
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be in range 1-65535");
        }

        socket = new DatagramSocket(new InetSocketAddress(port));
        socket.setSoTimeout(1);
    }

    public void stop() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = null;
        connectedClients.clear();
        clientPlayerIds.clear();
        clientReadyStates.clear();
        clientStates.clear();
        ownerClientId = null;
    }

    @Override
    public void run() {
        if (socket == null) {
            throw new IllegalStateException("Server socket was not initialized. Call start(port) first.");
        }
        running.set(true);

        long previousTime = System.nanoTime();
        double accumulator = 0.0;

        while (running.get()) {
            long now = System.nanoTime();
            double frameSeconds = (now - previousTime) / 1_000_000_000.0;
            previousTime = now;

            if (frameSeconds > MAX_FRAME_SECONDS) {
                frameSeconds = MAX_FRAME_SECONDS;
            }

            accumulator += frameSeconds;

            int ticksProcessed = 0;
            while (accumulator >= TICK_DT_SECONDS && ticksProcessed < MAX_TICKS_PER_FRAME) {
                runOneTick((float) TICK_DT_SECONDS);
                accumulator -= TICK_DT_SECONDS;
                ticksProcessed++;
            }

            long sleepNanos = (long) ((TICK_DT_SECONDS - accumulator) * 1_000_000_000L);
            if (sleepNanos > 0) {
                try {
                    long sleepMillis = sleepNanos / 1_000_000L;
                    int sleepNanoPart = (int) (sleepNanos % 1_000_000L);
                    Thread.sleep(sleepMillis, sleepNanoPart);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running.set(false);
                }
            }
        }
    }

    private void runOneTick(float tickDeltaSeconds) {
        processIncomingPackets();

        if (gameState == GameState.LOBBY) {
            sendLobbyState();
        } else if (gameState == GameState.PLAYING) {
            applyBufferedInputs();
            simulateWorld(tickDeltaSeconds);
            sendSnapshots();
        }
        serverTick++;
    }

    private void processIncomingPackets() {
        if (socket == null || socket.isClosed()) {
            return;
        }

        while (true) {
            try {
                byte[] buffer = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                if (message.isEmpty()) {
                    continue;
                }
                System.out.println("[SERVER] Received: " + message);

                handlePacket(message, packet.getSocketAddress());
            } catch (SocketTimeoutException timeout) {
                return;
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private void applyBufferedInputs() {
        Map<Integer, PlayerInput> latestInputs = new HashMap<>();
        PlayerInput input;
        while ((input = pendingInputs.poll()) != null) {
            Long processedTick = lastProcessedInputTick.get(input.playerId);
            if (processedTick != null && input.tick <= processedTick) {
                continue;
            }
            PlayerInput existing = latestInputs.get(input.playerId);
            if (existing == null || input.tick > existing.tick) {
                latestInputs.put(input.playerId, input);
            }
        }

        for (PlayerInput inp : latestInputs.values()) {
            for (ClientState state : clientStates.values()) {
                if (state.playerId == inp.playerId) {
                    float speed = 170f;
                    state.x += inp.moveX * speed * (float) TICK_DT_SECONDS;
                    state.y += inp.moveY * speed * (float) TICK_DT_SECONDS;
                    state.rotation = inp.rotation;
                    state.weaponName = inp.weaponName;
                    state.hp = inp.health;
                    lastProcessedInputTick.put(inp.playerId, inp.tick);
                    break;
                }
            }
        }
    }

    private void simulateWorld(float deltaSeconds) {
        for (Enemy enemy : enemies) {
            enemy.update(deltaSeconds);
        }

        for (Bullet bullet : bullets) {
            bullet.update(deltaSeconds);
            if (bullet.isExpired()) {
                bullets.remove(bullet);
                bullet.dispose();
            }
        }

        boolean anyDead = false;
        for (ClientState state : clientStates.values()) {
            if (state.hp <= 0) anyDead = true;
        }
        if (anyDead && gameState == GameState.PLAYING) {
            gameState = GameState.DEAD;
            broadcast("GAME_OVER");
        }

        clampPlayersToMap();
    }

    private void clampPlayersToMap() {
        float min = AREA_OFFSET;
        float max = PLAYABLE_AREA_SIZE - AREA_OFFSET + 100f;

        for (ClientState state : clientStates.values()) {
            state.x = Math.max(min, Math.min(max, state.x));
            state.y = Math.max(min, Math.min(max, state.y));
        }
    }

    private void sendSnapshots() {
        if (socket == null || socket.isClosed()) {
            return;
        }

        for (Map.Entry<String, SocketAddress> entry : connectedClients.entrySet()) {
            String clientId = entry.getKey();
            Integer playerId = clientPlayerIds.get(clientId);
            long lastAckTick = playerId == null ? -1L : lastProcessedInputTick.getOrDefault(playerId, -1L);
            ClientState self = clientStates.get(clientId);
            int selfId = self == null ? -1 : self.playerId;
            float selfX = self == null ? 0f : self.x;
            float selfY = self == null ? 0f : self.y;
            float selfHp = self == null ? 0f : self.hp;
            float selfRotation = self == null ? 0f : self.rotation;
            String selfWeapon = self == null ? "Gun" : self.weaponName;

            ClientState selfState = self == null ? null : clientStates.get(clientId);
            int selfGoldEarned = selfState == null ? 0 : selfState.goldEarned;
            int selfKills = selfState == null ? 0 : selfState.enemiesKilled;

            // Usuń całkowicie linię z selfGold
            StringBuilder payload = new StringBuilder("SNAPSHOT ")
                .append(serverTick).append(' ')
                .append(selfId).append(' ')
                .append(selfX).append(' ')
                .append(selfY).append(' ')
                .append(selfHp).append(' ')
                .append(selfRotation).append(' ')
                .append(selfWeapon).append(' ')
                .append(lastAckTick).append(' ')
                .append(clientStates.size());

            for (ClientState state : clientStates.values()) {
                payload.append(' ')
                    .append(state.playerId).append(',')
                    .append(state.x).append(',')
                    .append(state.y).append(',')
                    .append(state.hp).append(',')
                    .append(state.rotation).append(',')
                    .append(state.weaponName).append(',')
                    .append(state.goldEarned).append(',')
                    .append(state.enemiesKilled).append(',')
                    .append(state.speedBonus).append(',')
                    .append(state.maxHpBonus).append(',')
                    .append(state.damageBonus);
            }

            sendTo(entry.getValue(), payload.toString());
        }
    }

    private void handlePacket(String message, SocketAddress sender) {
        String[] parts = message.split("\\s+");
        if (parts.length == 0) {
            return;
        }

        if ("CONNECT".equalsIgnoreCase(parts[0]) && parts.length >= 2) {
            String clientId = parts[1];
            connectedClients.put(clientId, sender);
            int playerId = clientPlayerIds.computeIfAbsent(clientId, key -> nextPlayerId++);
            clientReadyStates.put(clientId, false);
            clientStates.computeIfAbsent(clientId, key -> {
                ClientState state = new ClientState();
                state.playerId = playerId;
                state.x = AREA_OFFSET + PLAYABLE_AREA_SIZE / 2f;
                state.y = AREA_OFFSET + PLAYABLE_AREA_SIZE / 2f;
                state.hp = 100f;
                state.weaponName = "Gun";
                state.goldEarned = 0;
                state.enemiesKilled = 0;
                return state;
            });
            if (ownerClientId == null) {
                ownerClientId = clientId;
            }
            sendTo(sender, "ACCEPT " + playerId);
            return;
        }

        if ("READY".equalsIgnoreCase(parts[0]) && parts.length >= 3) {
            String clientId = parts[1];
            if (!connectedClients.containsKey(clientId)) {
                return;
            }
            clientReadyStates.put(clientId, Boolean.parseBoolean(parts[2]));
            return;
        }

        if ("START".equalsIgnoreCase(parts[0]) && parts.length >= 2) {
            String clientId = parts[1];
            if (!clientId.equals(ownerClientId)) {
                return;
            }
            if (!allClientsReady()) {
                return;
            }
            gameState = GameState.PLAYING;
            broadcast("START");
            return;
        }

        if ("STATE".equalsIgnoreCase(parts[0]) && parts.length >= 4) {
            String clientId = parts[1];
            if (!clientId.equals(ownerClientId)) {
                return;
            }
            broadcast("STATE " + parts[2] + " " + parts[3]);
            return;
        }

        if ("SHOOT".equalsIgnoreCase(parts[0]) && parts.length >= 6) {
            if (gameState != GameState.PLAYING) {
                return;
            }
            String clientId = parts[1];
            if (!connectedClients.containsKey(clientId)) {
                return;
            }
            broadcast(message);
            return;
        }

        if ("WORLD".equalsIgnoreCase(parts[0]) && parts.length >= 2) {
            String clientId = parts[1];
            if (!clientId.equals(ownerClientId)) {
                return;
            }
            String prefix = "WORLD " + clientId + " ";
            if (message.startsWith(prefix)) {
                broadcast("WORLD " + message.substring(prefix.length()));
            }
            return;
        }

        if ("INPUT".equalsIgnoreCase(parts[0]) && parts.length >= 9) {
            if (gameState != GameState.PLAYING) {
                return;
            }
            String clientId = parts[1];
            Integer playerId = clientPlayerIds.get(clientId);
            if (playerId == null) {
                return;
            }

            try {
                long inputTick = Long.parseLong(parts[2]);
                float moveX = Float.parseFloat(parts[3]);
                float moveY = Float.parseFloat(parts[4]);
                boolean fire = Boolean.parseBoolean(parts[5]);
                float rotation = Float.parseFloat(parts[6]);
                String weaponName = parts[7];
                float health = Float.parseFloat(parts[8]);
                enqueueInput(new PlayerInput(playerId, inputTick, moveX, moveY, fire, rotation, weaponName, health));
            } catch (NumberFormatException ignored) {
                // Ignore malformed packets.
            }
        }

        if ("KILL".equalsIgnoreCase(parts[0]) && parts.length >= 2) {
            if (gameState != GameState.PLAYING) {
                return;
            }
            String clientId = parts[1];

            globalGold += GOLD_PER_KILL;

            ClientState state = clientStates.get(clientId);
            if (state != null) {
                state.enemiesKilled++;
                state.goldEarned += GOLD_PER_KILL;
                System.out.println("[SERVER] KILL from " + clientId + " (playerId=" + state.playerId +
                    ") -> globalGold=" + globalGold + " kills=" + state.enemiesKilled);
            }

            for (SocketAddress recipient : connectedClients.values()) {
                sendTo(recipient, "GOLD " + globalGold);
            }
            return;
        }

        if ("BUY".equalsIgnoreCase(parts[0]) && parts.length >= 4) {
            if (gameState != GameState.PLAYING && gameState != GameState.STORE) {
                return;
            }

            String clientId = parts[1];
            int price;
            String itemId;

            try {
                price = Integer.parseInt(parts[2]);
                itemId = parts[3];
            } catch (NumberFormatException e) {
                return;
            }


            if (globalGold < price) {

                sendTo(connectedClients.get(clientId), "BUY_REJECT " + itemId);
                System.out.println("[SERVER] BUY_REJECT from " + clientId + " - not enough gold (has: " + globalGold + ", needs: " + price + ")");
                return;
            }

            globalGold -= price;
            System.out.println("[SERVER] BUY_ACK from " + clientId + " - " + itemId + " for " + price + " gold. Remaining: " + globalGold);


            ClientState state = clientStates.get(clientId);
            if (state != null) {
                switch (itemId) {
                    case "GUN": state.weaponName = "Gun"; break;
                    case "SHOTGUN": state.weaponName = "Shotgun"; break;
                    case "UZI": state.weaponName = "Uzi"; break;
                    case "WINGED_BOOTS": state.speedBonus += ShopUI.BOOT_BOOST; break;
                    case "LIFE_FRUIT": state.maxHpBonus += ShopUI.LIFE_FRUIT_BOOST; state.hp = 100 + state.maxHpBonus; break;
                    case "STRENGTH_CHIP": state.damageBonus += ShopUI.CHIP_BOOST; break;
                }
            }

            for (SocketAddress recipient : connectedClients.values()) {
                sendTo(recipient, "BUY_ACK " + clientId + " " + globalGold + " " + itemId);
            }

            return;
        }
    }

    private boolean allClientsReady() {
        if (connectedClients.isEmpty()) {
            return false;
        }
        for (String clientId : connectedClients.keySet()) {
            if (!Boolean.TRUE.equals(clientReadyStates.get(clientId))) {
                return false;
            }
        }
        return true;
    }

    private void sendLobbyState() {
        if (socket == null || socket.isClosed()) {
            return;
        }

        List<Map.Entry<String, Integer>> orderedPlayers = new ArrayList<>(clientPlayerIds.entrySet());
        orderedPlayers.sort(Comparator.comparingInt(Map.Entry::getValue));

        StringBuilder payload = new StringBuilder("LOBBY ").append(orderedPlayers.size());
        for (Map.Entry<String, Integer> entry : orderedPlayers) {
            String clientId = entry.getKey();
            int playerId = entry.getValue();
            boolean ready = Boolean.TRUE.equals(clientReadyStates.get(clientId));
            payload.append(' ')
                .append(playerId)
                .append(',')
                .append(ready ? "1" : "0")
                .append(',')
                .append(clientId);
        }

        broadcast(payload.toString());
    }

    private void broadcast(String payload) {
        for (SocketAddress recipient : connectedClients.values()) {
            sendTo(recipient, payload);
        }
    }

    private void sendTo(SocketAddress recipient, String payload) {
        if (socket == null || socket.isClosed()) {
            return;
        }

        try {
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, recipient);
            socket.send(packet);
        } catch (IOException ignored) {
            // Sending failed for this packet.
        }
    }
}
