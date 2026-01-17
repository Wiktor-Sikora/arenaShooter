package io.github.arenaShooter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import io.github.arenaShooter.enemies.Zombie;
import io.github.arenaShooter.ui.playerHud;
import io.github.arenaShooter.ui.ShopUI;
import io.github.arenaShooter.weapons.Bullet;
import io.github.arenaShooter.weapons.Gun;
import io.github.arenaShooter.weapons.Shotgun;
import io.github.arenaShooter.weapons.Uzi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends ApplicationAdapter {
    public enum GameState {
        PLAYING,
        STORE
    }
    public GameState gameState = GameState.PLAYING;
    public int waveNumber = 0;
    private SpriteBatch batch;
    public OrthographicCamera camera;
    public ScreenViewport viewport;
    private Texture map;
    public Stage stage;

    private final float MAP_TEXTURE_SIZE = 1500;
    public final float PLAYABLE_AREA_SIZE = 1400;
    public final float AREA_OFFSET = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;

    float WORLD_WIDTH = 1500f;
    float WORLD_HEIGHT = 1500f;

    // NOWE: Stala ilosci zlota za zabicie
    private static final int GOLD_PER_KILL = 30;

    public Player player;
    public playerHud playerHud;
    public ShopUI shopUI;  // NOWE: Referencja do ShopUI
    public Array<Enemy> enemies;
    public Array<Bullet> bullets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.setUnitsPerPixel(1f);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        map = new Texture("map.png");

        player = new Player(AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, AREA_OFFSET + PLAYABLE_AREA_SIZE / 2, this);

        playerHud = new playerHud(this);
        shopUI = new ShopUI(this);  // NOWE: Inicjalizacja ShopUI

        enemies = new Array<>();

        bullets = new Array<>();

        camera.position.set(player.getCenterX(), player.getCenterY(), 0);

        startNextWave();
    }

    private void startNextWave() {
        final Random rand = new Random();

        waveNumber++;
        gameState = GameState.PLAYING;
        shopUI.randomizeShop();
        shopUI.resetWavePurchases();

        final List<java.util.function.Supplier<Enemy>> enemyFactory = List.of(
            () -> new Skeleton(0, 0, this),
            () -> new Zombie(0, 0, this)
        );

        float multiplier = 1f + (waveNumber - 1) * 0.1f; // +10% per wave

        // NOWE: Reset statystyk fali
        if (shopUI != null) {
            shopUI.resetStats();
        }

        System.out.println("=== WAVE " + waveNumber + "===");
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
        }else if (gameState == GameState.STORE) {

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

            for (int i = 0; i < enemies.size; i++) {
                if (enemies.get(i).isDisposable()) {
                    // NOWE: Zloto 30 za zabicie + statystyki
                    player.gold += GOLD_PER_KILL;
                    if (shopUI != null) {
                        shopUI.recordKill();
                        shopUI.recordGold(GOLD_PER_KILL);
                    }
                    System.out.println("Gold: " + player.gold);
                    enemies.get(i).dispose();
                    enemies.removeIndex(i);
                    i--;
                } else {
                    enemies.get(i).update(delta);
                }
            }

            if (enemies.size == 0) {
                gameState = GameState.STORE;
                System.out.println("=== SHOP ===");
            }
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

        camera.zoom = 0.8f;

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(map, 0, 0, MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE);

        player.render(batch);

        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).render(batch);
        }

        for (int i = 0; i < bullets.size; i++) {
            bullets.get(i).render(batch);
        }

        batch.end();

        // NOWE: Renderowanie sklepu gdy gracz jest w stanie STORE
        if (gameState == GameState.STORE && shopUI != null) {
            stage.draw();
            playerHud.render();
            shopUI.render();
        } else {
            stage.draw();
            playerHud.render();
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

        // NOWE: Dispose ShopUI
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
}
