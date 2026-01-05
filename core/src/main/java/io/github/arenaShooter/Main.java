package io.github.arenaShooter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
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
import io.github.arenaShooter.weapons.Bullet;

public class Main extends ApplicationAdapter {
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

    public Player player;
    public playerHud playerHud;
    public Array<Enemy> enemies;
    public Array<Bullet> bullets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera); //camera view
        viewport.setUnitsPerPixel(1f);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        map = new Texture("map.png");

        player = new Player(500f, 500f, this);

        playerHud = new playerHud(this);

        enemies = new Array<>();
        for (int i = 0; i < 3; i++) {
            enemies.add(new Skeleton((float)(Math.random() * PLAYABLE_AREA_SIZE), (float)(Math.random() * PLAYABLE_AREA_SIZE), this));
            enemies.add(new Zombie((float)(Math.random() * PLAYABLE_AREA_SIZE), (float)(Math.random() * PLAYABLE_AREA_SIZE), this));
        }

        bullets = new Array<>();

        camera.position.set(player.getCenterX(), player.getCenterY(), 0);
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    private void input() {
        float delta = Gdx.graphics.getDeltaTime();

        player.handleInput(delta);
    }

    private void logic() {
        float delta = Gdx.graphics.getDeltaTime();

        player.update(delta);

        for (int i = 0; i < enemies.size; i++) {
            if (enemies.get(i).isDisposable()) {
                enemies.get(i).dispose();
                enemies.removeIndex(i);
            } else {
                enemies.get(i).update(delta);
            }
        }

        for (int i = 0; i < bullets.size; i++) {
            if (bullets.get(i).isExpired()) {
                bullets.get(i).dispose();
                bullets.removeIndex(i);
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
