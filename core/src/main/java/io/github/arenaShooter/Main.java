package io.github.arenaShooter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.arenaShooter.enemies.Enemy;
import io.github.arenaShooter.enemies.Skeleton;
import io.github.arenaShooter.enemies.Slime;
import io.github.arenaShooter.enemies.Zombie;
import io.github.arenaShooter.ui.LobbyMenu;
import io.github.arenaShooter.ui.PlayerHud;
import io.github.arenaShooter.ui.ShopUI;
import io.github.arenaShooter.ui.Menu;
import io.github.arenaShooter.ui.ScoreboardMenu;
import io.github.arenaShooter.weapons.Bullet;
import io.github.arenaShooter.DatabaseManager;
import io.github.arenaShooter.weapons.Gun;
import io.github.arenaShooter.weapons.Shotgun;
import io.github.arenaShooter.weapons.Uzi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class Main extends ApplicationAdapter {
    public enum GameState {
        MENU,
        SCOREBOARD,
        LOBBY,
        PLAYING,
        PAUSED,
        DEAD,
        STORE
    }
    public GameState gameState = GameState.MENU;
    public int waveNumber = 0;
    private SpriteBatch batch;
    public OrthographicCamera camera;
    public ScreenViewport viewport;
    private Texture map;
    public Stage stage;
    private BitmapFont font;
    private GlyphLayout layout;

    public int playerSpeedBonus = 0;
    public int playerMaxHpBonus = 0;
    public int playerDamageBonus = 0;

    private DatabaseManager db;

    public ShapeRenderer shapeRenderer;

    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> walkAnimation;

    private float clientTargetX, clientTargetY;
    private float clientPrevX, clientPrevY;
    private float clientInterpTimer = 0f;
    private boolean clientMoving = false;

    private java.lang.reflect.Field playerCurrentFrameField;
    private java.lang.reflect.Field playerStateTimeField;

    private final float MAP_TEXTURE_SIZE = 1500;
    public final float PLAYABLE_AREA_SIZE = 1400;
    public final float AREA_OFFSET = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;

    float WORLD_WIDTH = 1500f;
    float WORLD_HEIGHT = 1500f;

    private static final int GOLD_PER_KILL = 30;

    public Player player;
    public PlayerHud playerHud;
    public ShopUI shopUI;
    public Array<Enemy> enemies;
    public Array<Bullet> bullets;

    public Gun defaultGun;
    public Shotgun shotgun;
    public Uzi uzi;

    public class HighScores {
        public int goldEarned;
        public int enemiesKilled;
        public int damageTaken;
    }

    public int globalGold = 0;
    public int globalGoldEarned = 0;

    private NetworkServer networkServer;
    private NetworkClient networkClient;
    private Thread networkServerThread;
    private boolean networkConnected = false;
    private long networkInputTick = 0L;
    public Menu menu;
    public ScoreboardMenu scoreboardMenu;
    private LobbyMenu lobbyMenu;
    private final CopyOnWriteArrayList<LobbyMenu.LobbyPlayer> lobbyPlayers = new CopyOnWriteArrayList<>();
    public boolean lobbyLocalReady = false;
    public String lobbyStatus = "Waiting for lobby state...";
    private GameState lastSyncedHostState = null;
    private int lastSyncedHostWave = -1;
    private Texture networkPlayerBulletTexture;
    private Texture networkEnemyBulletTexture;
    private TextureAtlas remotePlayerAtlas;
    private Animation<TextureRegion> sideWalkAnimation;
    private Animation<TextureRegion> frontWalkAnimation;
    private Animation<TextureRegion> backWalkAnimation;
    private TextureRegion remotePlayerFrame;
    private Map<String, Texture> remoteWeaponTextures = new HashMap<>();
    private Map<String, float[]> remoteWeaponDimensions = new HashMap<>();

    private static final String LAST_IP_FILE = "last_ip.txt";

    private void saveLastIp(String ip) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_IP_FILE))) {
            writer.write(ip);
        } catch (IOException e) {
            Gdx.app.error("Config", "Failed to save last IP", e);
        }
    }

    private String loadLastIp() {
        try (BufferedReader reader = new BufferedReader(new FileReader(LAST_IP_FILE))) {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static class EnemySnapshot {
        String type;
        float x;
        float y;
        float health;
        boolean dead;
    }

    private static class BulletSnapshot {
        String owner;
        float x;
        float y;
        float width;
        float height;
        float vx;
        float vy;
        float rotation;
    }

    private static class RemotePlayerSnapshot {
        int id;
        float x;
        float y;
        float hp;
        float rotation;
        String weaponName;
        Texture weaponTexture;
        float weaponTextureWidth;
        float weaponTextureHeight;
    }

    private static class RemotePlayerState {
        float displayX;
        float displayY;
        float displayRotation;

        float prevX;
        float prevY;

        float targetX;
        float targetY;
        float targetRotation;

        float interpolationTimer = 0f;
        float hp;
        String weaponName;
        boolean dead = false;
        int gold = 0;
        int goldEarned = 0;
        int enemiesKilled = 0;

        io.github.arenaShooter.ui.HealthBar healthBar;
        float stateTime = 0f;
        boolean isMoving = false;
    }

    private final Map<Integer, RemotePlayerState> remotePlayers = new ConcurrentHashMap<>();

    HighScores scores = new HighScores();

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.setUnitsPerPixel(1f);
        stage = new Stage(viewport);
        font = new BitmapFont();
        layout = new GlyphLayout();
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (gameState != GameState.MENU) {
                    return false;
                }
                return menu != null && menu.handleCharacter(character);
            }
        });
        Gdx.input.setInputProcessor(inputMultiplexer);
        shapeRenderer = new ShapeRenderer();

        map = new Texture("map.png");
        networkPlayerBulletTexture = new Texture("bullet.png");
        networkEnemyBulletTexture = new Texture("bone.png");

        remotePlayerAtlas = new TextureAtlas(Gdx.files.internal("player.atlas"));
        remotePlayerFrame = remotePlayerAtlas.findRegion("player_side_0");

        Array<TextureRegion> sideWalkFrames = new Array<>();
        for (int i = 0; i < 5; i++) {
            sideWalkFrames.add(remotePlayerAtlas.findRegion("player_side_" + i));
        }
        sideWalkAnimation = new Animation<>(0.2f, sideWalkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> frontWalkFrames = new Array<>();
        for (int i = 0; i < 3; i++) {
            frontWalkFrames.add(remotePlayerAtlas.findRegion("player_front_" + i));
        }
        frontWalkAnimation = new Animation<>(0.3f, frontWalkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> backWalkFrames = new Array<>();
        for (int i = 0; i < 3; i++) {
            backWalkFrames.add(remotePlayerAtlas.findRegion("player_back_" + i));
        }
        backWalkAnimation = new Animation<>(0.3f, backWalkFrames, Animation.PlayMode.LOOP);

        remoteWeaponTextures.put("Gun", new Texture("gun.png"));
        remoteWeaponDimensions.put("Gun", new float[]{20f, 17f});
        remoteWeaponTextures.put("Uzi", new Texture("uzi_icon.png"));
        remoteWeaponDimensions.put("Uzi", new float[]{20f, 17f});
        remoteWeaponTextures.put("Shotgun", new Texture("shotgun.png"));
        remoteWeaponDimensions.put("Shotgun", new float[]{33f, 14f});

        player = new Player(AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, this);

        try {
            playerStateTimeField = Player.class.getDeclaredField("stateTime");
            playerStateTimeField.setAccessible(true);
            playerCurrentFrameField = Player.class.getDeclaredField("currentFrame");
            playerCurrentFrameField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Gdx.app.error("Reflection", "Cannot access player fields", e);
        }

        defaultGun = new Gun(this);
        shotgun = new Shotgun(this);
        uzi = new Uzi(this);

        playerHud = new PlayerHud(this);
        shopUI = new ShopUI(this);

        enemies = new Array<>();

        bullets = new Array<>();

        camera.position.set(player.getCenterX(), player.getCenterY(), 0);

        String defaultClientHost = System.getProperty("arena.network.host", "127.0.0.1");
        int defaultPort = parsePort(System.getProperty("arena.network.port", "7777"), 7777);

        String savedIp = loadLastIp();
        if (savedIp != null && !savedIp.isBlank()) {
            defaultClientHost = savedIp;
        }
        menu = new Menu(this, defaultClientHost, resolveLocalHostingIp(), defaultPort);

        db = new DatabaseManager();
        scoreboardMenu = new ScoreboardMenu(db, font, layout, camera);
        scoreboardMenu.loadHighScores();
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    private void input() {
        float delta = Gdx.graphics.getDeltaTime();

        if (gameState == GameState.MENU) {
            menu.handleInput();
        }

        if (gameState == GameState.SCOREBOARD) {
            if (scoreboardMenu.handleInput()) {
                gameState = GameState.MENU;
            }
            return;
        }

        if (gameState == GameState.LOBBY) {
            lobbyMenu.handleInput();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                syncHostGameStateIfNeeded();
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
                syncHostGameStateIfNeeded();
            }
        }

        if (gameState == GameState.PLAYING) {
            if (menu.startMode == Menu.NetworkMode.CLIENT) {
                if (Gdx.input.isTouched(Input.Buttons.LEFT)) {
                    Vector3 unprojectedCords = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
                    Vector2 direction = new Vector2(unprojectedCords.x - player.getCenterX(), unprojectedCords.y - player.getCenterY());
                    player.weapon.setPlayerId(player.playerId);
                    player.weapon.shoot(direction);
                    sendNetworkShoot(player.getCenterX(), player.getCenterY(), direction.x, direction.y, player.weapon.name);
                }
            }
            sendNetworkInput();
            return;
        } else if (gameState == GameState.DEAD) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) returnToMenu();
        } else if (gameState == GameState.STORE) {

            if (menu.startMode == Menu.NetworkMode.HOST && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                startNextWave();
                return;
            }

            for (int i = 0; i < 4; i++) {
                int keyCode = Input.Keys.NUM_1 + i;
                if (Gdx.input.isKeyJustPressed(keyCode)) {
                    if (networkConnected) {
                        ShopUI.ShopItem item = shopUI.getCurrentItem(i);
                        if (item != null) {
                            sendNetworkBuy(item.getPrice(), item.getName());
                        }
                    } else {
                        shopUI.buyItem(i);
                    }
                    break;
                }
            }
        }
    }

    private void logic() {
        float delta = Gdx.graphics.getDeltaTime();

        if (gameState == GameState.PLAYING && menu.startMode == Menu.NetworkMode.CLIENT) {
            float interpDuration = 0.2f;
            clientInterpTimer += delta;
            float alpha = MathUtils.clamp(clientInterpTimer / interpDuration, 0f, 1f);
            float newX = MathUtils.lerp(clientPrevX, clientTargetX, alpha);
            float newY = MathUtils.lerp(clientPrevY, clientTargetY, alpha);
            player.hitbox.setPosition(newX, newY);

            Vector3 mousePos = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
            Vector2 direction = new Vector2(mousePos.x - player.getCenterX(), mousePos.y - player.getCenterY()).nor();
            float angle = direction.angleDeg();
            player.rotation = angle;
            player.facingLeft = (angle >= 135 && angle < 225);

            try {
                float stateTime;
                if (clientMoving) {
                    stateTime = playerStateTimeField.getFloat(player) + delta;
                    playerStateTimeField.setFloat(player, stateTime);
                } else {
                    stateTime = 4f;
                    playerStateTimeField.setFloat(player, stateTime);
                }

                TextureRegion frame;
                if (angle >= 45 && angle < 135) {
                    frame = backWalkAnimation.getKeyFrame(stateTime, clientMoving);
                } else if (angle >= 135 && angle < 225) {
                    frame = sideWalkAnimation.getKeyFrame(stateTime, clientMoving);
                } else if (angle >= 225 && angle < 315) {
                    frame = frontWalkAnimation.getKeyFrame(stateTime, clientMoving);
                } else {
                    frame = sideWalkAnimation.getKeyFrame(stateTime, clientMoving);
                }
                playerCurrentFrameField.set(player, frame);
            } catch (IllegalAccessException e) {
                Gdx.app.error("Reflection", "Failed to access player fields", e);
            }
        }

        if (gameState == GameState.PLAYING) {
            updateRemotePlayers(delta);

            if (menu.startMode != Menu.NetworkMode.CLIENT) {
                player.update(delta);
            }

            if (player.health <= 0) {
                player.healthBar.dispose();
                player.dispose();

                for (int i=0; i<enemies.size; i++ ) {
                    enemies.get(i).healthBar.dispose();
                }

                gameState = GameState.DEAD;
                syncHostGameStateIfNeeded();
                try {
                    this.saveScore();
                    this.loadScore();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            for (int i = 0; i < enemies.size; i++) {
                if (enemies.get(i).isDisposable()) {
                    globalGold += GOLD_PER_KILL;
                    globalGoldEarned += GOLD_PER_KILL;
                    player.enemiesKilled++;
                    System.out.println("[GOLD] Kill! " + (menu.startMode != null ? menu.startMode : "null") + " gold=" + player.gold + " enemies=" + player.enemiesKilled + " connected=" + networkConnected);
                    if (shopUI != null) {
                        shopUI.recordKill();
                        shopUI.recordGold(GOLD_PER_KILL);
                    }

                    if (networkConnected) {
                        sendNetworkKill();
                    }

                    enemies.get(i).dispose();
                    enemies.removeIndex(i);
                    i--;
                } else {
                    enemies.get(i).update(delta);
                }
            }

            if (menu.startMode != Menu.NetworkMode.CLIENT && enemies.size == 0) {
                gameState = GameState.STORE;
                syncHostGameStateIfNeeded();
            }

            for (int i = 0; i < bullets.size; i++) {
                if (bullets.get(i).isExpired()) {
                    bullets.get(i).dispose();
                    bullets.removeIndex(i);
                    i--;
                } else {
                    bullets.get(i).update(delta);
                }
            }

            if (menu.startMode == Menu.NetworkMode.HOST) {
                syncHostWorldSnapshot();
            }
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);

        if (gameState == GameState.MENU) {
            menu.render(batch, font, layout, camera);
            return;
        }

        if (gameState == GameState.SCOREBOARD) {
            scoreboardMenu.render(batch);
            return;
        }

        if (gameState == GameState.LOBBY) {
            lobbyMenu.render(batch, font, layout, camera, menu.startMode == Menu.NetworkMode.HOST, lobbyLocalReady, lobbyPlayers, lobbyStatus);
            return;
        }

        float delta = Gdx.graphics.getDeltaTime();
        updateRemotePlayers(delta);
        stage.act(Gdx.graphics.getDeltaTime());

        camera.position.x += (player.getCenterX() - camera.position.x) * 5f * delta;
        camera.position.y += (player.getCenterY() - camera.position.y) * 5f * delta;

        float quarterWidth = WORLD_WIDTH / 4f;
        float quarterHeight = WORLD_HEIGHT / 4f;

        camera.position.x = MathUtils.clamp(camera.position.x, quarterWidth, MAP_TEXTURE_SIZE - quarterWidth);
        camera.position.y = MathUtils.clamp(camera.position.y, quarterHeight, MAP_TEXTURE_SIZE - quarterHeight);

        if (Gdx.graphics.getWidth() > 1400) {
            camera.zoom = 0.8f;
        } else {
            camera.zoom = 1.1f;
        }

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(map, 0, 0, MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE);

        if (gameState == GameState.PLAYING) {
            player.render(batch);
            renderRemotePlayers(batch);
            for (int i = 0; i < enemies.size; i++) {
                enemies.get(i).render(batch);
            }

            for (int i = 0; i < bullets.size; i++) {
                bullets.get(i).render(batch);
            }

            batch.end();
            renderRemotePlayerHealthBars();
            stage.draw();
            playerHud.render();
        } else if (gameState == GameState.STORE && shopUI != null) {
            player.render(batch);
            renderRemotePlayers(batch);
            batch.end();
            renderRemotePlayerHealthBars();
            stage.draw();
            playerHud.render();
            shopUI.render();
        } else if (gameState == GameState.DEAD) {
            renderRemotePlayers(batch);
            for (int i = 0; i < enemies.size; i++) {
                enemies.get(i).render(batch);
            }

            for (int i = 0; i < bullets.size; i++) {
                bullets.get(i).render(batch);
            }

            float centerY = camera.position.y;

            font.setColor(Color.RED);
            drawCenteredText("You are dead", centerY + 20, 5f);

            font.setColor(Color.LIGHT_GRAY);
                drawCenteredText("Press [Esc] to quit", centerY - 55, 1.2f);
                drawCenteredText("Press [q] to return to menu", centerY - 80, 1.2f);

            drawCenteredText(String.format("Gold earned: %d / %d", player.goldEarned, scores.goldEarned), centerY - 130, 1.2f);
            drawCenteredText(String.format("Enemies killed: %d / %d", player.enemiesKilled, scores.enemiesKilled), centerY - 150, 1.2f);
            drawCenteredText(String.format("Damage taken: %d / %d", player.dmgTaken, scores.damageTaken), centerY - 170, 1.2f);

            batch.end();
            stage.draw();
        } else {
            batch.end();
        }

        if (gameState == GameState.PAUSED) {
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0.4f);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()+80);
            shapeRenderer.end();

            batch.begin();
            drawCenteredText("PAUSED", camera.position.y, 2f);
            drawCenteredText("Press ESC to play", camera.position.y-45, 1.2f);
            batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        shutdownNetworking();

        batch.dispose();
        player.dispose();
        map.dispose();
        networkPlayerBulletTexture.dispose();
        networkEnemyBulletTexture.dispose();
        remotePlayerAtlas.dispose();
        for (Texture tex : remoteWeaponTextures.values()) {
            tex.dispose();
        }
        stage.dispose();

        if (db != null) {
            db.close();
        }

        if (shopUI != null) {
            shopUI.dispose();
        }

        for (Enemy enemy: enemies) {
            enemy.dispose();
        }

        for (Bullet bullet: bullets) {
            bullet.dispose();
        }
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    private void startNextWave() {
        final Random rand = new Random();

        waveNumber++;
        gameState = GameState.PLAYING;
        syncHostGameStateIfNeeded();
        if (shopUI != null) {
            shopUI.randomizeShop();
            shopUI.resetWavePurchases();
        }

        if (menu.startMode == Menu.NetworkMode.CLIENT) {
            clearWorldCollections();
            return;
        }

        final List<Supplier<Enemy>> enemyFactory = List.of(
            () -> new Skeleton(0, 0, this),
            () -> new Zombie(0, 0, this),
            () -> new Slime(0, 0, this)
        );

        float multiplier = 1f + (waveNumber - 1) * 0.1f; // +10% per wave

        if (shopUI != null) {
            shopUI.resetStats();
        }

        int enemiesToSpawn = 2 + (waveNumber * 2 * (remotePlayers.size() + 1));

        for (int i = 0; i < enemiesToSpawn; i++) {
            float distanceToPlayer = 0;
            float x = 0;
            float y = 0;
            boolean tooClose;

            do {
                tooClose = false;
                x = (float)(AREA_OFFSET + Math.random() * PLAYABLE_AREA_SIZE);
                y = (float)(AREA_OFFSET + Math.random() * PLAYABLE_AREA_SIZE);

                float dx = player.hitbox.getX() - x;
                float dy = player.hitbox.getY() - y;
                distanceToPlayer = (float) Math.sqrt(dx * dx + dy * dy);
                if (distanceToPlayer < 500) tooClose = true;

                for (RemotePlayerState state : remotePlayers.values()) {
                    if (state.dead) continue;
                    dx = state.displayX - x;
                    dy = state.displayY - y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist < 500) {
                        tooClose = true;
                        break;
                    }
                }
            } while (tooClose);

            Enemy enemy = enemyFactory.get(rand.nextInt(enemyFactory.size())).get();
            enemy.hitbox.setPosition(x, y);
            enemy.speed = enemy.baseSpeed * multiplier;
            enemy.damage = enemy.baseDamage * multiplier;
            enemies.add(enemy);
        }
    }

    private void drawCenteredText(String text, float y, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        font.draw(batch, text, camera.position.x - layout.width / 2f, y);
    }

    private void saveScore() throws IOException {
        try {
            db.saveScore(player.goldEarned, player.enemiesKilled, player.dmgTaken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadScore() {
        try {
            List<String> list = db.getHighScores();
            if (list == null || list.isEmpty()) return;

            // Sort by gold
            list.sort((a, b) -> {
                String[] pa = a.trim().split("\\s+");
                String[] pb = b.trim().split("\\s+");
                int goldA = Integer.parseInt(pa[1].replaceAll(",", ""));
                int goldB = Integer.parseInt(pb[1].replaceAll(",", ""));
                return Integer.compare(goldB, goldA);
            });

            String best = list.get(0);
            String[] parts = best.trim().split("\\s+");

            if (parts.length >= 4) {
                scores.goldEarned = parseIntSafe(parts[1]);
                scores.enemiesKilled = parseIntSafe(parts[3]);
                scores.damageTaken = parseIntSafe(parts[6]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void returnToMenu() {
        player.dispose();
        player = new Player(AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, this);
        shopUI.dispose();
        shopUI = new ShopUI(this);
        waveNumber = 0;

        for (int i = 0; i < bullets.size; i++) {
            bullets.get(i).dispose();
            bullets.removeIndex(i);
            i--;
        }

        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).dispose();
            enemies.removeIndex(i);
            i--;
        }

        // reset camera
        float centerX = MAP_TEXTURE_SIZE / 2f;
        float centerY = MAP_TEXTURE_SIZE / 2f;
        camera.position.set(centerX, centerY, 0);
        camera.zoom = 1.0f;
        camera.update();

        menu.setStatus("TAB switch field, type value. F1 Host, F2 Join");
        gameState = GameState.MENU;
    }

    private void shutdownNetworking() {
        if (networkClient != null) {
            networkClient.disconnect();
        }

        if (networkServer != null) {
            networkServer.stop();
        }

        if (networkServerThread != null) {
            try {
                networkServerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendNetworkInput() {
        if (!networkConnected || networkClient == null || !networkClient.isConnected()) {
            return;
        }

        float moveX = 0f;
        float moveY = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1f;

        boolean fire = Gdx.input.isTouched(Input.Buttons.LEFT);
        String weaponName = player.weapon != null ? player.weapon.name : "Gun";
        String payload = "INPUT " + networkClient.getClientId() + " " + networkInputTick + " " + moveX + " " + moveY + " " + fire + " " + player.rotation + " " + weaponName + " " + player.health;
        try {
            networkClient.sendMessage(payload);
            networkInputTick++;
        } catch (IOException e) {
            networkConnected = false;
            Gdx.app.error("Networking", "Failed to send input packet", e);
        }
    }

    public void sendNetworkShoot(float x, float y, float dirX, float dirY, String weaponName) {
        if (!networkConnected || networkClient == null || !networkClient.isConnected()) {
            return;
        }
        try {
            networkClient.sendMessage("SHOOT " + networkClient.getClientId() + " " + x + " " + y + " " + dirX + " " + dirY + " " + weaponName);
        } catch (IOException e) {
            networkConnected = false;
        }
    }

    public void sendNetworkKill() {
        if (!networkConnected || networkClient == null || !networkClient.isConnected()) {
            System.out.println("[NET] sendNetworkKill: not connected");
            return;
        }
        try {
            String msg = "KILL " + networkClient.getClientId();
            System.out.println("[NET] Sending: " + msg);
            networkClient.sendMessage(msg);
        } catch (IOException e) {
            networkConnected = false;
        }
    }

    public void sendNetworkBuy(int price, String itemId) {
        if (!networkConnected || networkClient == null || !networkClient.isConnected()) {
            return;
        }
        try {
            networkClient.sendMessage("BUY " + networkClient.getClientId() + " " + price + " " + itemId);
        } catch (IOException e) {
            networkConnected = false;
        }
    }

    private void handleNetworkMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (message.startsWith("LOBBY ")) {
            handleLobbyMessage(message);
            return;
        }
        if (message.startsWith("START")) {
            Gdx.app.postRunnable(() -> {
                if (gameState == GameState.LOBBY) {
                    startNextWave();
                }
            });
            return;
        }
        if (message.startsWith("STATE ")) {
            handleStateMessage(message);
            return;
        }
        if (message.startsWith("WORLD ")) {
            handleWorldMessage(message);
            return;
        }
        if (message.startsWith("SHOOT ")) {
            handleShootMessage(message);
            return;
        }
        if (message.startsWith("GOLD ")) {
            handleGoldMessage(message);
            return;
        }
        if (message.startsWith("BUY_ACK ") || message.startsWith("BUY_REJECT ")) {
            handleBuyMessage(message);
            return;
        }
        if (message.startsWith("GAME_OVER")) {
            Gdx.app.postRunnable(() -> gameState = GameState.DEAD);
            return;
        }
        if (!message.startsWith("SNAPSHOT ")) {
            return;
        }

        handleSnapshotMessage(message);
    }

    private int parsePort(String rawPort, int defaultPort) {
        if (rawPort == null || rawPort.isBlank()) {
            return defaultPort;
        }

        try {
            int parsed = Integer.parseInt(rawPort);
            if (parsed > 0 && parsed <= 65535) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }

        return defaultPort;
    }

    public void startFromMenu(Menu.NetworkMode selectedMode) {
        shutdownNetworking();
        networkConnected = false;
        networkInputTick = 0L;
        lobbyLocalReady = false;
        lobbyPlayers.clear();
        remotePlayers.clear();
        lobbyStatus = "Connecting...";

        networkClient = new NetworkClient();
        networkClient.addMessageListener(this::handleNetworkMessage);

        if (selectedMode == Menu.NetworkMode.HOST) {
            networkServer = new NetworkServer();
            lobbyMenu = new LobbyMenu(this, String.format("%s:%s", this.resolveLocalHostingIp(), menu.getPort()));

            try {
                networkServer.start(menu.getPort());
                networkServerThread = new Thread(networkServer, "network-server");
                networkServerThread.setDaemon(true);
                networkServerThread.start();
            } catch (SocketException e) {
                Gdx.app.error("Networking", "Could not start host server on port " + menu.getPort(), e);
                menu.setStatus("Host failed. Press F1 to retry or F2 to join.");
                return;
            }

            try {
                networkConnected = networkClient.connect("127.0.0.1", menu.getPort());
            } catch (IOException e) {
                Gdx.app.error("Networking", "Host client could not connect to local server", e);
                menu.setStatus("Host started, but local client connection failed.");
                networkConnected = false;
            }
        } else if (selectedMode == Menu.NetworkMode.CLIENT) {
            try {
                String clientHost = menu.getClientHost();
                saveLastIp(clientHost);
                networkConnected = networkClient.connect(menu.getClientHost(), menu.getPort());
                lobbyMenu = new LobbyMenu(this, String.format("%s:%s", menu.getClientHost(), menu.getPort()));
            } catch (IOException e) {
                Gdx.app.error("Networking", "Client could not connect to " + menu.getClientHost() + ":" + menu.getPort(), e);
                menu.setStatus("Connection failed. Press F2 to retry or F1 to host.");
                networkConnected = false;
                return;
            }

            if (!networkConnected) {
                menu.setStatus("Connection refused/time out. Press F2 to retry or F1 to host.");
                return;
            }
        }

        if (!networkConnected) {
            return;
        }

        gameState = GameState.LOBBY;
        lobbyStatus = "Connected. Press R to set ready.";
        sendReadyState();
        lastSyncedHostState = null;
        lastSyncedHostWave = -1;
    }

    private String resolveLocalHostingIp() {
        List<String> localAddresses = NetworkClient.getLocalIpv4Addresses();
        if (localAddresses.isEmpty()) {
            return "127.0.0.1";
        }
        return localAddresses.get(0);
    }

    public void sendReadyState() {
        if (!networkConnected || networkClient == null || !networkClient.isConnected()) {
            return;
        }

        try {
            networkClient.sendMessage("READY " + networkClient.getClientId() + " " + lobbyLocalReady);
        } catch (IOException e) {
            networkConnected = false;
            lobbyStatus = "Disconnected while sending ready state.";
        }
    }

    public void requestLobbyStart() {
        if (!networkConnected || networkClient == null || !networkClient.isConnected()) {
            return;
        }

        try {
            networkClient.sendMessage("START " + networkClient.getClientId());
            lobbyStatus = "Starting game...";
        } catch (IOException e) {
            networkConnected = false;
            lobbyStatus = "Could not request game start.";
        }
    }

    private void handleLobbyMessage(String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 2) {
            return;
        }

        int playerCount;
        try {
            playerCount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return;
        }

        List<LobbyMenu.LobbyPlayer> parsedPlayers = new ArrayList<>();
        for (int i = 0; i < playerCount && i + 2 < parts.length; i++) {
            String[] playerParts = parts[i + 2].split(",", 3);
            if (playerParts.length < 3) {
                continue;
            }

            try {
                int playerId = Integer.parseInt(playerParts[0]);
                boolean ready = "1".equals(playerParts[1]);
                String clientId = playerParts[2];
                parsedPlayers.add(new LobbyMenu.LobbyPlayer(playerId, ready, clientId));

                if (networkClient != null && clientId.equals(networkClient.getClientId())) {
                    lobbyLocalReady = ready;
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed lobby entry.
            }
        }

        parsedPlayers.sort(Comparator.comparingInt(player -> player.playerId));
        lobbyPlayers.clear();
        lobbyPlayers.addAll(parsedPlayers);

        if (menu.startMode == Menu.NetworkMode.HOST) {
            if (areAllLobbyPlayersReady()) {
                lobbyStatus = "Everyone is ready. Press Enter to start.";
            } else {
                lobbyStatus = "Waiting for all clients to be ready...";
            }
        } else {
            lobbyStatus = areAllLobbyPlayersReady()
                ? "Everyone is ready. Waiting for host to start."
                : "Waiting for players to be ready...";
        }
    }

    public boolean areAllLobbyPlayersReady() {
        if (lobbyPlayers.isEmpty()) {
            return false;
        }
        for (LobbyMenu.LobbyPlayer player : lobbyPlayers) {
            if (!player.ready) {
                return false;
            }
        }
        return true;
    }

    private void handleStateMessage(String message) {
        if (menu.startMode != Menu.NetworkMode.CLIENT) return;

        String[] parts = message.split("\\s+");
        if (parts.length < 3) return;

        try {
            GameState incomingState = GameState.valueOf(parts[1]);
            int incomingWave = Integer.parseInt(parts[2]);
            System.out.println("[CLIENT] State message: " + incomingState + " wave " + incomingWave);

            Gdx.app.postRunnable(() -> {
                waveNumber = incomingWave;
                gameState = incomingState;
                if (incomingState == GameState.STORE && shopUI != null) {
                    shopUI.randomizeShop();
                    shopUI.resetWavePurchases();
                    System.out.println("[CLIENT] Shop randomized");
                }
            });
        } catch (IllegalArgumentException ignored) {
            System.out.println("[CLIENT] Invalid state message: " + message);
        }
    }

    private void syncHostGameStateIfNeeded() {
        if (menu.startMode != Menu.NetworkMode.HOST || !networkConnected || networkClient == null || !networkClient.isConnected()) {
            return;
        }
        if (gameState == GameState.MENU || gameState == GameState.LOBBY) {
            return;
        }
        if (lastSyncedHostState == gameState && lastSyncedHostWave == waveNumber) {
            return;
        }

        try {
            networkClient.sendMessage("STATE " + networkClient.getClientId() + " " + gameState.name() + " " + waveNumber);
            lastSyncedHostState = gameState;
            lastSyncedHostWave = waveNumber;
        } catch (IOException e) {
            networkConnected = false;
            lobbyStatus = "Disconnected while syncing state.";
        }
    }

    public boolean isClientNetworkMode() {
        return menu.startMode == Menu.NetworkMode.CLIENT;
    }

    private void syncHostWorldSnapshot() {
        if (menu.startMode != Menu.NetworkMode.HOST || !networkConnected || networkClient == null || !networkClient.isConnected() || gameState != GameState.PLAYING) {
            return;
        }

        StringBuilder payload = new StringBuilder("WORLD ");
        payload.append(networkClient.getClientId()).append(' ');
        payload.append(enemies.size).append(' ');

        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            payload.append(enemyTypeCode(enemy)).append(',')
                .append(enemy.hitbox.x).append(',')
                .append(enemy.hitbox.y).append(',')
                .append(enemy.health).append(',')
                .append(!enemy.isAlive()).append(' ');
        }

        payload.append("B ").append(bullets.size).append(' ');
        for (int i = 0; i < bullets.size; i++) {
            Bullet bullet = bullets.get(i);
            payload.append(bullet.getOwner() == Bullet.Owner.PLAYER ? "P" : "E").append(',')
                .append(bullet.hitbox.x).append(',')
                .append(bullet.hitbox.y).append(',')
                .append(bullet.hitbox.width).append(',')
                .append(bullet.hitbox.height).append(',')
                .append(bullet.getVelocity().x).append(',')
                .append(bullet.getVelocity().y).append(',')
                .append(bullet.getRotation()).append(' ');
        }

        try {
            networkClient.sendMessage(payload.toString().trim());
        } catch (IOException e) {
            networkConnected = false;
            lobbyStatus = "Disconnected while syncing world.";
        }
    }

    private String enemyTypeCode(Enemy enemy) {
        if (enemy instanceof Skeleton) {
            return "K";
        }
        if (enemy instanceof Zombie) {
            return "Z";
        }
        return "S";
    }

    private void handleShootMessage(String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 6) {
            return;
        }
        String shooterId = parts[1];
        if (shooterId.equals(networkClient.getClientId())) {
            return;
        }
        try {
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            float dirX = Float.parseFloat(parts[4]);
            float dirY = Float.parseFloat(parts[5]);
            String weaponName = parts.length > 6 ? parts[6] : "Gun";
            final float finalX = x;
            final float finalY = y;
            final Vector2 direction = new Vector2(dirX, dirY);
            final String finalWeaponName = weaponName;
            Gdx.app.postRunnable(() -> {
                createRemotePlayerBullet(finalX, finalY, direction, finalWeaponName);
            });
        } catch (NumberFormatException ignored) {
        }
    }

    private void createRemotePlayerBullet(float x, float y, Vector2 direction, String weaponName) {
        Texture tex = networkPlayerBulletTexture;
        float damage = 20f;
        float speed = 500f;
        float range = 300f;

        int width = 5;
        int height = 15;

        if ("Uzi".equals(weaponName)) {
            damage = 20f;
            range = 500f;
        } else if ("Shotgun".equals(weaponName)) {
            damage = 8f;
            range = 500f;
            width = 5;
            height = 15;
        }

        addBullet(new Bullet(this, x, y, direction, tex, width, height, damage, speed, range, Bullet.Owner.PLAYER));
    }

    private void handleGoldMessage(String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 2) return;
        try {
            int newGold = Integer.parseInt(parts[1]);
            Gdx.app.postRunnable(() -> {
                globalGold = newGold;
                if (shopUI != null) shopUI.updateGold(globalGold);
                if (playerHud != null) playerHud.updateGold(globalGold);
            });
        } catch (NumberFormatException ignored) {}
    }

    private void handleBuyMessage(String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 2) return;

        if (message.startsWith("BUY_REJECT ")) {
            Gdx.app.postRunnable(() -> {
                if (shopUI != null) shopUI.showPurchaseRejected();
            });
            return;
        }

        if (message.startsWith("BUY_ACK ") && parts.length >= 4) {
            final String buyerId = parts[1];
            final int newGold = Integer.parseInt(parts[2]);
            final String itemId = parts[3];

            System.out.println("[CLIENT] BUY_ACK: buyer=" + buyerId + ", myId=" + (networkClient != null ? networkClient.getClientId() : "null"));

            Gdx.app.postRunnable(() -> {
                globalGold = newGold;
                if (shopUI != null) shopUI.updateGold(globalGold);
                if (playerHud != null) playerHud.updateGold(globalGold);

                if (networkClient != null && buyerId.equals(networkClient.getClientId())) {
                    System.out.println("[CLIENT] Applying purchase for " + buyerId);
                    if (!"HEALTH POTION".equals(itemId)) {
                        shopUI.applyPurchase(itemId, Main.this);
                    }
                    sendNetworkInput();
                } else {
                    System.out.println("[CLIENT] Skipping purchase application – not the buyer");
                }
            });
        }
    }

    private void handleWorldMessage(String message) {
        if (menu.startMode != Menu.NetworkMode.CLIENT) {
            return;
        }
        String[] parts = message.split("\\s+");
        if (parts.length < 4) {
            return;
        }

        int index = 1;
        int enemyCount;
        try {
            enemyCount = Integer.parseInt(parts[index++]);
        } catch (NumberFormatException ignored) {
            return;
        }

        List<EnemySnapshot> enemySnapshots = new ArrayList<>();
        for (int i = 0; i < enemyCount && index < parts.length; i++) {
            String[] enemyParts = parts[index++].split(",", 5);
            if (enemyParts.length < 5) {
                continue;
            }
            try {
                EnemySnapshot snapshot = new EnemySnapshot();
                snapshot.type = enemyParts[0];
                snapshot.x = Float.parseFloat(enemyParts[1]);
                snapshot.y = Float.parseFloat(enemyParts[2]);
                snapshot.health = Float.parseFloat(enemyParts[3]);
                snapshot.dead = Boolean.parseBoolean(enemyParts[4]);
                enemySnapshots.add(snapshot);
            } catch (NumberFormatException ignored) {
                return;
            }
        }

        if (index >= parts.length || !"B".equals(parts[index])) {
            return;
        }
        index++;
        if (index >= parts.length) {
            return;
        }

        int bulletCount;
        try {
            bulletCount = Integer.parseInt(parts[index++]);
        } catch (NumberFormatException ignored) {
            return;
        }

        List<BulletSnapshot> bulletSnapshots = new ArrayList<>();
        for (int i = 0; i < bulletCount && index < parts.length; i++) {
            String[] bulletParts = parts[index++].split(",", 8);
            if (bulletParts.length < 8) {
                continue;
            }
            try {
                BulletSnapshot snapshot = new BulletSnapshot();
                snapshot.owner = bulletParts[0];
                snapshot.x = Float.parseFloat(bulletParts[1]);
                snapshot.y = Float.parseFloat(bulletParts[2]);
                snapshot.width = Float.parseFloat(bulletParts[3]);
                snapshot.height = Float.parseFloat(bulletParts[4]);
                snapshot.vx = Float.parseFloat(bulletParts[5]);
                snapshot.vy = Float.parseFloat(bulletParts[6]);
                snapshot.rotation = Float.parseFloat(bulletParts[7]);
                bulletSnapshots.add(snapshot);
            } catch (NumberFormatException ignored) {
                return;
            }
        }

        Gdx.app.postRunnable(() -> applyWorldSnapshot(enemySnapshots, bulletSnapshots));
    }

    private void applyWorldSnapshot(List<EnemySnapshot> enemySnapshots, List<BulletSnapshot> bulletSnapshots) {
        reconcileEnemies(enemySnapshots);
        reconcileBullets(bulletSnapshots);
    }

    private void reconcileEnemies(List<EnemySnapshot> snapshots) {
        for (int i = 0; i < snapshots.size(); i++) {
            EnemySnapshot snapshot = snapshots.get(i);
            Enemy enemy = i < enemies.size ? enemies.get(i) : null;

            if (enemy == null || !enemyTypeCode(enemy).equals(snapshot.type)) {
                if (enemy != null) {
                    enemy.dispose();
                    enemies.removeIndex(i);
                }
                Enemy created = createEnemyByType(snapshot.type, snapshot.x, snapshot.y);
                created.health = snapshot.health;
                if (snapshot.dead) {
                    created.setDeadState();
                }
                enemies.insert(i, created);
                continue;
            }

            enemy.hitbox.setPosition(snapshot.x, snapshot.y);
            enemy.health = snapshot.health;
            if (snapshot.dead && enemy.isAlive()) {
                enemy.setDeadState();
            }
        }

        while (enemies.size > snapshots.size()) {
            Enemy removed = enemies.pop();
            removed.dispose();
        }
    }

    private Enemy createEnemyByType(String type, float x, float y) {
        Enemy enemy;
        if ("K".equals(type)) {
            enemy = new Skeleton(x, y, this);
        } else if ("Z".equals(type)) {
            enemy = new Zombie(x, y, this);
        } else {
            enemy = new Slime(x, y, this);
        }
        enemy.isRemote = true;
        return enemy;
    }

    private void reconcileBullets(List<BulletSnapshot> snapshots) {
        for (int i = 0; i < snapshots.size(); i++) {
            BulletSnapshot snapshot = snapshots.get(i);
            Bullet bullet = i < bullets.size ? bullets.get(i) : null;
            Bullet.Owner owner = "P".equals(snapshot.owner) ? Bullet.Owner.PLAYER : Bullet.Owner.ENEMY;

            if (bullet == null || bullet.getOwner() != owner) {
                if (bullet != null) {
                    bullet.dispose();
                    bullets.removeIndex(i);
                }
                bullets.insert(i, createBulletFromSnapshot(snapshot, owner));
                continue;
            }

            bullet.setPosition(snapshot.x, snapshot.y);
            bullet.setSize(snapshot.width, snapshot.height);
            bullet.setVelocity(snapshot.vx, snapshot.vy);
            bullet.setRotation(snapshot.rotation);
        }

        while (bullets.size > snapshots.size()) {
            Bullet removed = bullets.pop();
            removed.dispose();
        }
    }

    private Bullet createBulletFromSnapshot(BulletSnapshot snapshot, Bullet.Owner owner) {
        Texture texture = owner == Bullet.Owner.PLAYER ? networkPlayerBulletTexture : networkEnemyBulletTexture;
        Vector2 velocity = new Vector2(snapshot.vx, snapshot.vy);
        float speed = velocity.len();
        Vector2 direction = speed == 0f ? new Vector2(1, 0) : new Vector2(velocity).nor();

        Bullet bullet = new Bullet(
            this,
            snapshot.x,
            snapshot.y,
            direction,
            texture,
            (int) snapshot.width,
            (int) snapshot.height,
            0f,
            speed,
            99999f,
            owner
        );
        bullet.setVelocity(snapshot.vx, snapshot.vy);
        bullet.setRotation(snapshot.rotation);
        return bullet;
    }

    private void clearWorldCollections() {
        for (int i = 0; i < bullets.size; i++) {
            bullets.get(i).dispose();
        }
        bullets.clear();

        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).dispose();
        }
        enemies.clear();
    }

    private void handleSnapshotMessage(String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 10) return;

        try {
            int myId = Integer.parseInt(parts[2]);
            float myX = Float.parseFloat(parts[3]);
            float myY = Float.parseFloat(parts[4]);
            float myHp = Float.parseFloat(parts[5]);

            if (networkClient != null && myId == networkClient.getPlayerId()) {
                if (menu.startMode == Menu.NetworkMode.CLIENT) {
                    clientPrevX = player.hitbox.x;
                    clientPrevY = player.hitbox.y;
                    clientTargetX = myX;
                    clientTargetY = myY;
                    clientInterpTimer = 0f;
                    float dx = myX - clientPrevX;
                    float dy = myY - clientPrevY;
                    clientMoving = (dx * dx + dy * dy) > 0.25f;
                    player.health = myHp;
                }
            }

            int playerCount = Integer.parseInt(parts[9]);
            int playerStartIndex = 10;
            Set<Integer> currentRemoteIds = new HashSet<>();


            for (int i = 0; i < playerCount; i++) {
                int index = playerStartIndex + i;
                if (index >= parts.length) break;

                String[] pData = parts[index].split(",");
                if (pData.length < 11) continue;

                int remoteId = Integer.parseInt(pData[0]);
                currentRemoteIds.add(remoteId);

                float rx = Float.parseFloat(pData[1]);
                float ry = Float.parseFloat(pData[2]);
                float rhp = Float.parseFloat(pData[3]);
                float rrot = Float.parseFloat(pData[4]);
                String rweapon = pData[5];
                int rGoldEarned = Integer.parseInt(pData[6]);
                int rEnemiesKilled = Integer.parseInt(pData[7]);
                int rSpeedBonus = Integer.parseInt(pData[8]);
                int rMaxHpBonus = Integer.parseInt(pData[9]);
                int rDamageBonus = Integer.parseInt(pData[10]);

                if (networkClient != null && remoteId == networkClient.getPlayerId()) {
                    currentRemoteIds.add(remoteId);
                    playerSpeedBonus = rSpeedBonus;
                    playerMaxHpBonus = rMaxHpBonus;
                    playerDamageBonus = rDamageBonus;
                    player.speed = 170f + playerSpeedBonus;
                    player.maxHealth = 100 + playerMaxHpBonus;
                    player.dmg = playerDamageBonus;
                    if (player.weapon != null) {
                        player.weapon.damage = player.weapon.damage + playerDamageBonus;
                    }
                    final String newWeapon = rweapon;
                    if (!player.weapon.name.equals(newWeapon)) {
                        Gdx.app.postRunnable(() -> {
                            switch (newWeapon) {
                                case "Gun":
                                    player.equipWeapon(defaultGun);
                                    break;
                                case "Shotgun":
                                    player.equipWeapon(shotgun);
                                    break;
                                case "Uzi":
                                    player.equipWeapon(uzi);
                                    break;
                            }
                            player.weapon.damage += player.dmg;
                        });
                    }
                    player.goldEarned = rGoldEarned;
                    player.enemiesKilled = rEnemiesKilled;
                    continue;
                }

                RemotePlayerState state = remotePlayers.get(remoteId);
                if (state == null) {
                    final RemotePlayerState newState = new RemotePlayerState();
                    newState.displayX = rx;
                    newState.displayY = ry;
                    newState.hp = rhp;
                    newState.targetX = rx;
                    newState.targetY = ry;
                    newState.isMoving = false;
                    newState.stateTime = 4f;
                    Gdx.app.postRunnable(() -> {
                        newState.healthBar = new io.github.arenaShooter.ui.HealthBar(this, 100, 64);
                    });
                    remotePlayers.put(remoteId, newState);
                    state = newState;
                }

                float oldDisplayX = state.displayX;
                float oldDisplayY = state.displayY;
                state.prevX = oldDisplayX;
                state.prevY = oldDisplayY;
                state.targetX = rx;
                state.targetY = ry;
                state.targetRotation = rrot;
                state.hp = rhp;
                state.weaponName = rweapon;
                state.dead = (rhp <= 0);
                state.interpolationTimer = 0f;

                float dx = rx - oldDisplayX;
                float dy = ry - oldDisplayY;
                boolean wasMoving = state.isMoving;
                state.isMoving = (dx * dx + dy * dy) > 0.25f;
                if (!state.isMoving && wasMoving) {
                    state.stateTime = 4f;
                }
            }

            for (Integer id : new ArrayList<>(remotePlayers.keySet())) {
                if (!currentRemoteIds.contains(id)) {
                    RemotePlayerState removed = remotePlayers.remove(id);
                    if (removed != null && removed.healthBar != null) {
                        Gdx.app.postRunnable(() -> removed.healthBar.dispose());
                    }
                }
            }

        } catch (Exception e) {
            Gdx.app.error("Network", "Błąd parsowania snapshotu: " + message, e);
        }
    }

    private void updateRemotePlayers(float delta) {
        float interpolationDuration = 0.2f;

        for (RemotePlayerState state : remotePlayers.values()) {
            if (state.dead) continue;

            state.interpolationTimer += delta;
            float alpha = MathUtils.clamp(state.interpolationTimer / interpolationDuration, 0f, 1f);

            state.displayX = MathUtils.lerp(state.prevX, state.targetX, alpha);
            state.displayY = MathUtils.lerp(state.prevY, state.targetY, alpha);
            state.displayRotation = MathUtils.lerpAngleDeg(state.displayRotation, state.targetRotation, 15f * delta);

            if (state.isMoving) {
                state.stateTime += delta;
            } else {
                state.stateTime = 4f;
            }
        }
    }

    private void renderRemotePlayers(SpriteBatch batch) {
        for (RemotePlayerState state : remotePlayers.values()) {
            if (state.dead) continue;

            float angle = state.displayRotation;
            boolean remoteFacingLeft = false;
            TextureRegion frame;

            if (angle >= 45 && angle < 135) {
                frame = backWalkAnimation.getKeyFrame(state.stateTime, state.isMoving);
                remoteFacingLeft = false;
            } else if (angle >= 135 && angle < 225) {
                frame = sideWalkAnimation.getKeyFrame(state.stateTime, state.isMoving);
                remoteFacingLeft = true;
            } else if (angle >= 225 && angle < 315) {
                frame = frontWalkAnimation.getKeyFrame(state.stateTime, state.isMoving);
                remoteFacingLeft = false;
            } else {
                frame = sideWalkAnimation.getKeyFrame(state.stateTime, state.isMoving);
                remoteFacingLeft = false;
            }

            boolean weaponBehind = (angle > 45 && angle < 135);
            if (weaponBehind) drawRemoteWeapon(batch, state);

            float drawX = state.displayX;
            float drawY = state.displayY;
            float tw = 64f;
            float th = 64f;

            if (remoteFacingLeft) {
                batch.draw(frame, drawX + tw, drawY, -tw, th);
            } else {
                batch.draw(frame, drawX, drawY, tw, th);
            }

            if (!weaponBehind) drawRemoteWeapon(batch, state);
        }
    }

    private void drawRemoteWeapon(SpriteBatch batch, RemotePlayerState state) {
        if (state.weaponName == null || !remoteWeaponTextures.containsKey(state.weaponName)) return;

        Texture tex = remoteWeaponTextures.get(state.weaponName);
        float[] dims = remoteWeaponDimensions.get(state.weaponName);
        if (tex == null || dims == null) return;

        float textureWidth = dims[0];
        float textureHeight = dims[1];

        // 1. Obliczanie kierunku (Logika 4-kierunkowa z Twoich klas Weapon)
        float angle = state.displayRotation;
        if (angle < 0) angle += 360;
        float renderRotation = 0f;
        boolean flipped = false;

        if ((angle >= 0 && angle <= 45) || (angle > 315 && angle <= 360)) {
            renderRotation = 0; flipped = false; // Prawo
        } else if (angle > 135 && angle <= 225) {
            renderRotation = 0; flipped = true;  // Lewo
        } else if (angle > 45 && angle <= 135) {
            renderRotation = 90; // Góra
        } else {
            renderRotation = -90; // Dół
        }

        float offsetX = 0;
        float offsetY = 0;

        float centerX = state.displayX + 32f;
        float centerY = state.displayY + 32f;

        if (renderRotation == 90) {
            offsetX = -10;
            offsetY = 25;
        } else if (renderRotation == -90) {
            offsetX = 0;
            offsetY = -15;
        } else {

            switch (state.weaponName) {
                case "Shotgun":
                    offsetX = flipped ? -28 : -3;
                    offsetY = -5;
                    break;
                case "Uzi":
                    offsetX = flipped ? -20 : 0;
                    offsetY = -5;
                    break;
                case "Gun":
                default:
                    offsetX = flipped ? -28 : 7;
                    offsetY = -5;
                    break;
            }
        }

        // 3. Rysowanie finalne
        batch.draw(
            tex,
            centerX + offsetX,
            centerY + offsetY,
            textureWidth / 2, textureHeight / 2, // Origin w środku tekstury broni
            textureWidth, textureHeight,
            1f, 1f,
            renderRotation,
            0, 0,
            tex.getWidth(), tex.getHeight(),
            flipped, false
        );
    }

    private void renderRemotePlayerHealthBars() {
        for (RemotePlayerState state : remotePlayers.values()) {
            if (state.dead) {
                if (state.healthBar != null) state.healthBar.dispose();
                continue;
            }

            if (state.healthBar != null) {
                state.healthBar.render(state.hp, state.displayX, state.displayY);
            }
        }
    }

    public Vector2 getClosestPlayerCenter(float fromX, float fromY) {
        Vector2 closest = new Vector2(player.getCenterX(), player.getCenterY());
        float bestDistance2 = (closest.x - fromX) * (closest.x - fromX) + (closest.y - fromY) * (closest.y - fromY);

        for (RemotePlayerState state : remotePlayers.values()) {
            if (state.dead) continue;
            float candidateX = state.displayX + 16f;
            float candidateY = state.displayY + 32f;
            float distance2 = (candidateX - fromX) * (candidateX - fromX) + (candidateY - fromY) * (candidateY - fromY);
            if (distance2 < bestDistance2) {
                bestDistance2 = distance2;
                closest.set(candidateX, candidateY);
            }
        }

        return closest;
    }

    public boolean isAnyPlayerOverlapping(Rectangle hitbox) {
        if (hitbox.overlaps(player.hitbox)) {
            return true;
        }
        for (RemotePlayerState state : remotePlayers.values()) {
            if (state.dead) continue;
            Rectangle remoteHitbox = new Rectangle(state.displayX, state.displayY, player.hitbox.width, player.hitbox.height);
            if (hitbox.overlaps(remoteHitbox)) {
                return true;
            }
        }
        return false;
    }

    public void damageClosestLocalPlayerIfTargeted(float fromX, float fromY, float amount) {
        Vector2 closest = getClosestPlayerCenter(fromX, fromY);
        float epsilon = 0.1f;
        if (Math.abs(closest.x - player.getCenterX()) < epsilon && Math.abs(closest.y - player.getCenterY()) < epsilon) {
            player.takeDamage(amount);
        } else {
            for (RemotePlayerState state : remotePlayers.values()) {
                if (state.dead) continue;
                float remoteCenterX = state.displayX + 16f;
                float remoteCenterY = state.displayY + 32f;
                if (Math.abs(closest.x - remoteCenterX) < epsilon && Math.abs(closest.y - remoteCenterY) < epsilon) {
                    state.hp -= amount;
                    break;
                }
            }
        }
    }
}
