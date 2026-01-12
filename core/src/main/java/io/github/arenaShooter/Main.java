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

        player = new Player(500f, 500f, new Texture("dummy.png"), this);

        playerHud = new playerHud(this);
        shopUI = new ShopUI(this);  // NOWE: Inicjalizacja ShopUI

        enemies = new Array<>();
        for (int i = 0; i < 3; i++) {
            enemies.add(new Skeleton((float)(Math.random() * 501), (float)(Math.random() * 501), this));
            enemies.add(new Zombie((float)(Math.random() * 501), (float)(Math.random() * 501), this));
        }

        bullets = new Array<>();

        camera.position.set(player.getCenterX(), player.getCenterY(), 0);

        startNextWave();
    }

    private void startNextWave() {
        waveNumber++;
        gameState = GameState.PLAYING;

        // NOWE: Reset statystyk fali
        if (shopUI != null) {
            shopUI.resetStats();
        }

        System.out.println("=== FALA " + waveNumber + " ROZPOCZETA ===");
        int enemiesToSpawn = 2 + (waveNumber * 2);

        for (int i = 0; i < enemiesToSpawn; i++) {
            float x = (float)(Math.random() * 501);
            float y = (float)(Math.random() * 501);

            if (Math.random() > 0.5) {
                enemies.add(new Skeleton(x, y, this));
            } else {
                enemies.add(new Zombie(x, y, this));
            }
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
        } else if (gameState == GameState.STORE) {
            // NOWE: Obsluga sklepu
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                startNextWave();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                player.equipWeapon(new Gun(this));
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                if (player.gold >= ShopUI.SHOTGUN_PRICE) {
                    player.gold -= ShopUI.SHOTGUN_PRICE;
                    player.equipWeapon(new Shotgun(this));
                    System.out.println("Kupiono Shotgun! Zloto: " + player.gold);
                } else {
                    System.out.println("Za malo zlota na Shotgun (" + ShopUI.SHOTGUN_PRICE + ")");
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                if (player.gold >= ShopUI.UZI_PRICE) {
                    player.gold -= ShopUI.UZI_PRICE;
                    player.equipWeapon(new Uzi(this));
                    System.out.println("Kupiono Uzi! Zloto: " + player.gold);
                } else {
                    System.out.println("Za malo zlota na Uzi (" + ShopUI.UZI_PRICE + ")");
                }
            }
            // NOWE: Kupowanie mikstury HP
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                if (player.gold >= ShopUI.POTION_PRICE) {
                    if (player.health < player.maxHealth) {
                        player.gold -= ShopUI.POTION_PRICE;
                        player.health = Math.min(player.health + ShopUI.POTION_HEAL, player.maxHealth);
                        System.out.println("Kupiono miksture! HP: " + (int)player.health + "/" + (int)player.maxHealth);
                    } else {
                        System.out.println("Masz pelne HP!");
                    }
                } else {
                    System.out.println("Za malo zlota na miksture (" + ShopUI.POTION_PRICE + ")");
                }
            }
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
                    System.out.println("Zloto: " + player.gold);
                    enemies.get(i).dispose();
                    enemies.removeIndex(i);
                    i--;
                } else {
                    enemies.get(i).update(delta);
                }
            }

            if (enemies.size == 0) {
                gameState = GameState.STORE;
                System.out.println("=== SKLEP ===");
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

        playerHud.render();
        stage.draw();

        // NOWE: Renderowanie sklepu gdy gracz jest w stanie STORE
        if (gameState == GameState.STORE && shopUI != null) {
            shopUI.render();
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
