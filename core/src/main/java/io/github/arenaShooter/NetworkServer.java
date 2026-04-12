package io.github.arenaShooter;

import io.github.arenaShooter.enemies.Enemy;
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

    public static class PlayerInput {
        public final int playerId;
        public final long tick;
        public final float moveX;
        public final float moveY;
        public final boolean fire;
        public final float rotation;

        public PlayerInput(int playerId, long tick, float moveX, float moveY, boolean fire, float rotation) {
            this.playerId = playerId;
            this.tick = tick;
            this.moveX = moveX;
            this.moveY = moveY;
            this.fire = fire;
            this.rotation = rotation;
        }
    }

    public GameState gameState = GameState.LOBBY;

    public CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Enemy> enemies = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();
    public ArrayList<BulletData> syncedBullets = new ArrayList<>();

    public static class EnemyData {
        float x;
        float y;
        float health;
        byte type;
    }

    public static class BulletData {
        float x;
        float y;
        float width;
        float height;
        float vx;
        float vy;
        float rotation;
        byte owner;
        float distanceTraveled;
    }

    private List<EnemyData> syncedEnemies = new ArrayList<>();

    private final float MAP_TEXTURE_SIZE = 1500;
    public final float PLAYABLE_AREA_SIZE = 1400;
    public final float AREA_OFFSET = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;
    float WORLD_WIDTH = 1500f;
    float WORLD_HEIGHT = 1500f;

    private static final long TICK_RATE_HZ = 20L;
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
    private final Map<Integer, Long> lastFireTick = new ConcurrentHashMap<>();

    private static final long FIRE_COOLDOWN_TICKS = 10L;

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
    }

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

                handlePacket(message, packet.getSocketAddress());
            } catch (SocketTimeoutException timeout) {
                return;
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private void applyBufferedInputs() {
        PlayerInput input;
        while ((input = pendingInputs.poll()) != null) {
            Long processedTick = lastProcessedInputTick.get(input.playerId);
            if (processedTick != null && input.tick <= processedTick) {
                continue;
            }

            for (ClientState state : clientStates.values()) {
                if (state.playerId == input.playerId) {
                    float speed = 170f;
                    state.x += input.moveX * speed * (float) TICK_DT_SECONDS;
                    state.y += input.moveY * speed * (float) TICK_DT_SECONDS;
                    state.rotation = input.rotation;

                    Long lastFire = lastFireTick.get(input.playerId);
                    if (input.fire && (lastFire == null || input.tick - lastFire >= FIRE_COOLDOWN_TICKS)) {
                        spawnBullet(state.x, state.y, state.rotation, (byte)0);
                        lastFireTick.put(input.playerId, input.tick);
                    }
                    break;
                }
            }

            lastProcessedInputTick.put(input.playerId, input.tick);
        }
    }

    private void simulateWorld(float deltaSeconds) {
        for (EnemyData enemy : syncedEnemies) {
            enemyAI(enemy, deltaSeconds);
        }

        updateBullets(deltaSeconds);

        clampPlayersToMap();
    }

    private void enemyAI(EnemyData enemy, float deltaSeconds) {
        float speed = 50f;
        float targetX = AREA_OFFSET + PLAYABLE_AREA_SIZE / 2f;
        float targetY = AREA_OFFSET + PLAYABLE_AREA_SIZE / 2f;
        
        for (ClientState state : clientStates.values()) {
            targetX = state.x;
            targetY = state.y;
            break;
        }
        
        float dx = targetX - enemy.x;
        float dy = targetY - enemy.y;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 0) {
            enemy.x += (dx / dist) * speed * deltaSeconds;
            enemy.y += (dy / dist) * speed * deltaSeconds;
        }
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

            StringBuilder payload = new StringBuilder("SNAPSHOT ")
                .append(serverTick).append(' ')
                .append(selfId).append(' ')
                .append(selfX).append(' ')
                .append(selfY).append(' ')
                .append(selfHp).append(' ')
                .append(selfRotation).append(' ')
                .append(lastAckTick).append(' ')
                .append(clientStates.size());

            for (ClientState state : clientStates.values()) {
                payload.append(' ')
                    .append(state.playerId).append(',')
                    .append(state.x).append(',')
                    .append(state.y).append(',')
                    .append(state.hp).append(',')
                    .append(state.rotation);
            }

            payload.append(" E ").append(syncedEnemies.size());
            for (EnemyData enemy : syncedEnemies) {
                char typeChar = enemyTypeChar(enemy.type);
                payload.append(' ').append(typeChar).append(',')
                    .append(enemy.x).append(',')
                    .append(enemy.y).append(',')
                    .append(enemy.health);
            }

            payload.append(" B ").append(syncedBullets.size());
            for (BulletData bullet : syncedBullets) {
                payload.append(' ').append(bullet.owner == 0 ? 'P' : 'E').append(',')
                    .append(bullet.x).append(',')
                    .append(bullet.y).append(',')
                    .append(bullet.width).append(',')
                    .append(bullet.height).append(',')
                    .append(bullet.vx).append(',')
                    .append(bullet.vy).append(',')
                    .append(bullet.rotation);
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
            spawnEnemies(1);
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

        if ("INPUT".equalsIgnoreCase(parts[0]) && parts.length >= 7) {
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
                enqueueInput(new PlayerInput(playerId, inputTick, moveX, moveY, fire, rotation));
            } catch (NumberFormatException ignored) {
                // Ignore malformed packets.
            }
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

    private void spawnEnemies(int waveNumber) {
        int playerCount = Math.max(1, connectedClients.size());
        int enemiesToSpawn = 2 + (waveNumber * 2 * playerCount);

        syncedEnemies.clear();
        for (int i = 0; i < enemiesToSpawn; i++) {
            EnemyData data = new EnemyData();
            data.x = AREA_OFFSET + (float)(Math.random() * PLAYABLE_AREA_SIZE);
            data.y = AREA_OFFSET + (float)(Math.random() * PLAYABLE_AREA_SIZE);
            data.type = (byte)(Math.random() * 3);
            data.health = 100f;
            syncedEnemies.add(data);
        }
    }

    private char enemyTypeChar(byte type) {
        if (type == 0) return 'K';
        if (type == 1) return 'Z';
        return 'S';
    }

    private void spawnBullet(float x, float y, float rotation, byte owner) {
        BulletData bullet = new BulletData();
        float speed = 400f;
        float rad = (float)Math.toRadians(rotation);
        bullet.vx = (float)Math.cos(rad) * speed;
        bullet.vy = (float)Math.sin(rad) * speed;
        bullet.x = x;
        bullet.y = y;
        bullet.width = 8f;
        bullet.height = 8f;
        bullet.rotation = rotation;
        bullet.owner = owner;
        bullet.distanceTraveled = 0f;
        syncedBullets.add(bullet);
    }

    private void updateBullets(float deltaSeconds) {
        float speed = 400f;
        float maxRange = 300f;
        for (int i = syncedBullets.size() - 1; i >= 0; i--) {
            BulletData b = syncedBullets.get(i);
            float dist = (float)Math.sqrt(b.vx * b.vx + b.vy * b.vy) * deltaSeconds;
            b.distanceTraveled += dist;
            b.x += b.vx * deltaSeconds;
            b.y += b.vy * deltaSeconds;
            if (b.distanceTraveled > maxRange ||
                b.x < AREA_OFFSET || b.x > AREA_OFFSET + PLAYABLE_AREA_SIZE ||
                b.y < AREA_OFFSET || b.y > AREA_OFFSET + PLAYABLE_AREA_SIZE) {
                syncedBullets.remove(i);
            }
        }
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
