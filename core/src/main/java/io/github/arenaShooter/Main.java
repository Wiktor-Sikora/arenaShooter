package io.github.arenaShooter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;


public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private Texture player;
    private Texture map;

    private float playerX = 500;
    private float playerY = 500;
    private float playerSpeed = 300;
    private final float MAP_TEXTURE_SIZE = 1100;
    private final float PLAYABLE_AREA_SIZE = 1000;
    private final float PLAYER_MARGIN = 28;

    private final float AREA_OFFSET_X = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;
    private final float AREA_OFFSET_Y = (MAP_TEXTURE_SIZE - PLAYABLE_AREA_SIZE) / 2f;

    private Enemy enemy;
    private TextureAtlas atlasSkeleton;
    private TextureAtlas atlasDeath;


    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(750, 700, camera); //camera view (zoomed)

        player = new Texture("dummy.png");
        map = new Texture("map.png");


        atlasSkeleton = new TextureAtlas(Gdx.files.internal("skeleton.atlas"));
        atlasDeath = new TextureAtlas(Gdx.files.internal("death.atlas"));
        enemy = new Enemy((float)(Math.random() * 501), (float)(Math.random() * 501), atlasSkeleton, atlasDeath);


        camera.position.set(playerX, playerY, 0);
        camera.update();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 0); //black bg

        float delta = com.badlogic.gdx.Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) playerY += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) playerY -= playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) playerX -= playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) playerX += playerSpeed * delta;

        //player does not exceed the border of the map
        playerX = MathUtils.clamp(playerX,
            AREA_OFFSET_X + PLAYER_MARGIN,
            AREA_OFFSET_X + PLAYABLE_AREA_SIZE - PLAYER_MARGIN);
        playerY = MathUtils.clamp(playerY,
            AREA_OFFSET_Y + PLAYER_MARGIN,
            AREA_OFFSET_Y + PLAYABLE_AREA_SIZE - PLAYER_MARGIN);


        camera.position.x += (playerX - camera.position.x) * 5f * delta;
        camera.position.y += (playerY - camera.position.y) * 5f * delta;

        float halfWidth = viewport.getWorldWidth() / 2f;
        float halfHeight = viewport.getWorldHeight() / 2f;

        camera.position.x = MathUtils.clamp(camera.position.x, halfWidth, MAP_TEXTURE_SIZE - halfWidth);
        camera.position.y = MathUtils.clamp(camera.position.y, halfHeight, MAP_TEXTURE_SIZE - halfHeight);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        if (enemy != null) {
            enemy.update(delta, playerX, playerY);
        }

        batch.begin();
        batch.draw(map, 0, 0, MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE);
        batch.draw(player, playerX - 32, playerY - 32, 64, 64);
        if (enemy != null) {
            enemy.render(batch);
        }
        batch.end();

        if (enemy != null) {
            if (!enemy.isAlive()) {
                if (enemy.isDeathAnimationFinished()) {
                    enemy.dispose();
                    enemy = null;
                }
            }
        }
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
        map.dispose();
        if (enemy != null) {
            enemy.dispose();
        }
    }
}
