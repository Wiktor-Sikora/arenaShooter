package io.github.arenaShooter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import io.github.arenaShooter.ui.HealthBar;


public class Player extends Entity {
    public float speed;
    public boolean alive = true;
    public Texture texture;

    public Player(float startX, float startY, Texture texture, Main game) {
        this.hitbox = new Rectangle(startX, startY, 64, 64);

        this.maxHealth = 100;
        this.health = 100;

        this.speed = 250f;
        this.texture = texture;
        this.game = game;

        this.healthBar = new HealthBar(game, maxHealth, (int)hitbox.width);
    }

    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);

        batch.draw(texture, hitbox.getX(), hitbox.getY(), hitbox.width, hitbox.height);
    }

    @Override
    public void update(float delta) {
        //player does not exceed the border of the map
        hitbox.setX(MathUtils.clamp(hitbox.getX(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET));
        hitbox.setY(MathUtils.clamp(hitbox.getY(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET));
    }

    public void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) hitbox.setY(hitbox.getY() + speed * delta);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) hitbox.setY(hitbox.getY() - speed * delta);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) hitbox.setX(hitbox.getX() - speed * delta);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) hitbox.setX(hitbox.getX() + speed * delta);
    }

    public void dispose() {
        super.dispose();

        if (texture != null) {
            texture.dispose();
        }
    }

    @Override
    public void kill() {

    }
}
