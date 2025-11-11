package io.github.arenaShooter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ScreenViewport viewport;
    private Texture map;

    private Player player;
    private final float MAP_TEXTURE_SIZE = 1500;
    private final float PLAYABLE_AREA_SIZE = 1400;
    private final float PLAYER_MARGIN = 28;

    float WORLD_WIDTH = 1000f;
    float WORLD_HEIGHT = 1000f;

    private final float AREA_OFFSET_X = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;
    private final float AREA_OFFSET_Y = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;

    private Array<Enemy> enemies;
    private TextureAtlas atlasSkeleton;
    private TextureAtlas atlasDeath;


    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera); //camera view
        viewport.setUnitsPerPixel(1f);

        map = new Texture("map.png");

        atlasSkeleton = new TextureAtlas(Gdx.files.internal("skeleton.atlas"));
        atlasDeath = new TextureAtlas(Gdx.files.internal("death.atlas"));

        player = new Player(500f, 500f, new Texture("dummy.png"));

        enemies = new Array<>();
        for (int i = 0; i < 3; i++) {
            enemies.add(new Enemy((float)(Math.random() * 501), (float)(Math.random() * 501), atlasSkeleton, atlasDeath));
        }

        camera.position.set(player.x, player.y, 0);
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    private void input() {
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) player.y += player.speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) player.y -= player.speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) player.x -= player.speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) player.x += player.speed * delta;
    }

    private void logic() {
        float delta = Gdx.graphics.getDeltaTime();

        //player does not exceed the border of the map
        player.x = MathUtils.clamp(player.x,
            AREA_OFFSET_X + PLAYER_MARGIN,
            AREA_OFFSET_X + PLAYABLE_AREA_SIZE - PLAYER_MARGIN);
        player.y = MathUtils.clamp(player.y,
            AREA_OFFSET_Y + PLAYER_MARGIN,
            AREA_OFFSET_Y + PLAYABLE_AREA_SIZE - PLAYER_MARGIN);

        for (int i = 0; i < enemies.size; i++) {
            if (enemies.get(i).isDeathAnimationFinished()) {
                enemies.get(i).dispose();
                enemies.removeIndex(i);
            } else {
                enemies.get(i).update(delta, player.x, player.y);
            }
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);

        float delta = Gdx.graphics.getDeltaTime();

        camera.position.x += (player.x - camera.position.x) * 5f * delta;
        camera.position.y += (player.y - camera.position.y) * 5f * delta;

        float quarterWidth = WORLD_WIDTH / 4f;
        float quarterHeight = WORLD_HEIGHT / 4f;

        camera.position.x = MathUtils.clamp(camera.position.x, quarterWidth, MAP_TEXTURE_SIZE - quarterWidth);
        camera.position.y = MathUtils.clamp(camera.position.y, quarterHeight, MAP_TEXTURE_SIZE - quarterHeight);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(map, 0, 0, MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE);
        batch.draw(player.texture, player.x - 32, player.y - 32, 64, 64);

        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).render(batch);
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        map.dispose();

        for (Enemy enemy: enemies) {
            enemy.dispose();
        }
    }
}
