package io.github.arenaShooter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.arenaShooter.enemies.Enemy;
import io.github.arenaShooter.enemies.Skeleton;
import io.github.arenaShooter.enemies.Slime;
import io.github.arenaShooter.enemies.Zombie;
import io.github.arenaShooter.ui.HealthBar;
import io.github.arenaShooter.ui.PlayerHud;
import io.github.arenaShooter.ui.ShopUI;
import io.github.arenaShooter.weapons.Bullet;

import java.lang.Math;
import java.io.*;
import java.util.Dictionary;
import java.util.List;
import java.util.Random;

public class Main extends ApplicationAdapter {
    public enum GameState {
        PLAYING,
        PAUSED,
        DEAD,
        STORE
    }
    public GameState gameState = GameState.PLAYING;
    public int waveNumber = 0;
    private SpriteBatch batch;
    public OrthographicCamera camera;
    public ScreenViewport viewport;
    private Texture map;
    public Stage stage;
    private BitmapFont font;
    private GlyphLayout layout;

    private ShapeRenderer shapeRenderer;

    private final float MAP_TEXTURE_SIZE = 1500;
    public final float PLAYABLE_AREA_SIZE = 1400;
    public final float AREA_OFFSET = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;

    float WORLD_WIDTH = 1500f;
    float WORLD_HEIGHT = 1500f;

    private static final int GOLD_PER_KILL = 30;

    private final String SCORE_FILE_NAME = "score.txt";

    public Player player;
    public PlayerHud playerHud;
    public ShopUI shopUI;
    public Array<Enemy> enemies;
    public Array<Bullet> bullets;

    public class HighScores {
        public int goldEarned;
        public int enemiesKilled;
        public int damageTaken;
    };
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
        Gdx.input.setInputProcessor(stage);

        map = new Texture("map.png");

        player = new Player(AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, this);

        playerHud = new PlayerHud(this);
        shopUI = new ShopUI(this);

        enemies = new Array<>();

        bullets = new Array<>();

        camera.position.set(player.getCenterX(), player.getCenterY(), 0);

        startNextWave();
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    private void input() {
        float delta = Gdx.graphics.getDeltaTime();

        if (gameState == GameState.PLAYING) {
            player.handleInput(delta);
            return;
        } else if (gameState == GameState.PAUSED) {
            if (Gdx.input.isButtonJustPressed(Input.Keys.ESCAPE)) {
                return;
            }
        } else if (gameState == GameState.DEAD) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) restartGame();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
        } else if (gameState == GameState.STORE) {

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                startNextWave();
                return;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) shopUI.buyItem(0);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) shopUI.buyItem(1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) shopUI.buyItem(2);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) shopUI.buyItem(3);
        }
    }

    private void logic() {
        float delta = Gdx.graphics.getDeltaTime();

        if (gameState == GameState.PLAYING) {
            player.update(delta);

            if (player.health <= 0) {
                player.healthBar.dispose();
                player.dispose();

                for (int i=0; i<enemies.size; i++ ) {
                    enemies.get(i).healthBar.dispose();
                }

                gameState = GameState.DEAD;
                try {
                    this.loadScore();
                    this.saveScore();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < enemies.size; i++) {
                if (enemies.get(i).isDisposable()) {
                    player.gold += GOLD_PER_KILL;

                    if (shopUI != null) {
                        shopUI.recordKill();
                        player.goldEarned += GOLD_PER_KILL;
                        player.enemiesKilled++;
                        shopUI.recordGold(GOLD_PER_KILL);
                    }

                    enemies.get(i).dispose();
                    enemies.removeIndex(i);
                    i--;
                } else {
                    enemies.get(i).update(delta);
                }
            }

            if (enemies.size == 0) {
                gameState = GameState.STORE;
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
        } else if (gameState == GameState.DEAD) {

        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);

        float delta = Gdx.graphics.getDeltaTime();
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
            for (int i = 0; i < enemies.size; i++) {
                enemies.get(i).render(batch);
            }

            for (int i = 0; i < bullets.size; i++) {
                bullets.get(i).render(batch);
            }

            batch.end();
            stage.draw();
            playerHud.render();
        } else if (gameState == GameState.STORE && shopUI != null) {
            player.render(batch);
            batch.end();
            stage.draw();
            playerHud.render();
            shopUI.render();
        } else if (gameState == GameState.DEAD) {
            for (int i = 0; i < enemies.size; i++) {
                enemies.get(i).render(batch);
            }

            for (int i = 0; i < bullets.size; i++) {
                bullets.get(i).render(batch);
            }

            float centerY = camera.position.y;

            font.setColor(Color.RED);
            drawCenteredText("You are dead", centerY + 10, 5f);

            font.setColor(Color.LIGHT_GRAY);
            drawCenteredText("Press [Enter] to restart", centerY - 55, 1.2f);
            drawCenteredText("Press [Esc] to quit", centerY - 75, 1.2f);

            drawCenteredText(String.format("Gold earned: %d / %d", player.goldEarned, scores.goldEarned), centerY - 110, 1.2f);
            drawCenteredText(String.format("Enemies killed: %d / %d", player.enemiesKilled, scores.enemiesKilled), centerY - 130, 1.2f);
            drawCenteredText(String.format("Damage taken: %d / %d", player.dmgTaken, scores.damageTaken), centerY - 150, 1.2f);

            batch.end();
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        map.dispose();
        stage.dispose();

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
        shopUI.randomizeShop();
        shopUI.resetWavePurchases();

        final List<java.util.function.Supplier<Enemy>> enemyFactory = List.of(
            () -> new Skeleton(0, 0, this),
            () -> new Zombie(0, 0, this),
            () -> new Slime(0, 0, this)
        );

        float multiplier = 1f + (waveNumber - 1) * 0.1f; // +10% per wave

        // NOWE: Reset statystyk fali
        if (shopUI != null) {
            shopUI.resetStats();
        }

        int enemiesToSpawn = 2 + (waveNumber * 2);

        for (int i = 0; i < enemiesToSpawn; i++) {
            float x = (float)(AREA_OFFSET + Math.random() * PLAYABLE_AREA_SIZE);
            float y = (float)(AREA_OFFSET + Math.random() * PLAYABLE_AREA_SIZE);

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
        BufferedWriter writer = new BufferedWriter(new FileWriter(SCORE_FILE_NAME));

        writer.write(String.format("%d;", Math.max(player.goldEarned, scores.goldEarned)));
        writer.write(String.format("%d;", Math.max(player.enemiesKilled, scores.enemiesKilled)));
        writer.write(String.format("%d", Math.max(player.dmgTaken, scores.damageTaken)));

        writer.close();
    }

    private void loadScore() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SCORE_FILE_NAME));
            String currentLine = reader.readLine();
            reader.close();

            if (currentLine == null) {
                scores.goldEarned = 0;
                scores.enemiesKilled = 0;
                scores.damageTaken = 0;
                return;
            };
            String[] values = currentLine.split(";");

            scores.goldEarned = Integer.parseInt(values[0]);
            scores.enemiesKilled = Integer.parseInt(values[1]);
            scores.damageTaken = Integer.parseInt(values[2]);
        } catch (IOException e) {
        }
    }

    private void restartGame() {
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

        startNextWave();
    }
}
