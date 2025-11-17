package io.github.arenaShooter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Player extends Entity {
    public float speed;
    public boolean alive = true;
    public Texture texture;
    float areaOffset;
    float areaSize;

    public Player(float startX, float startY, Texture texture, float areaOffset, float areaSize) {
        this.position = new Vector2(startX, startY);
        this.speed = 250f;
        this.texture = texture;
        this.width = 64;
        this.height = 64;

        this.areaOffset = areaOffset;
        this.areaSize = areaSize;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(texture, position.x, position.y, width, height);
    }

    @Override
    public void update(float delta) {
        //player does not exceed the border of the map
        position.x = MathUtils.clamp(position.x, areaOffset, areaSize - areaOffset);
        position.y = MathUtils.clamp(position.y, areaOffset, areaSize - areaOffset);
    }

    public void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) position.y += speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) position.y -= speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) position.x -= speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) position.x += speed * delta;
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
