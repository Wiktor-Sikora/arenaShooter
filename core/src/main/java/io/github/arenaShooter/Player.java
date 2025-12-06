package io.github.arenaShooter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import io.github.arenaShooter.ui.HealthBar;
import io.github.arenaShooter.weapons.Gun;
import io.github.arenaShooter.weapons.Weapon;


public class Player extends Entity {
    public float speed;
    public boolean alive = true;
    public Texture texture;

    public Weapon weapon;

    public Player(float startX, float startY, Texture texture, Main game) {
        this.textureHeight = textureWidth = 64;
        this.hitboxHeight = textureHeight;
        this.hitboxWidth = textureWidth / 2;
        this.hitbox = new Rectangle(startX, startY, hitboxWidth, hitboxHeight);

        this.maxHealth = 100;
        this.health = 100;

        this.speed = 250f;
        this.texture = texture;
        this.game = game;

        this.weapon = new Gun(game);

        this.healthBar = new HealthBar(game, maxHealth, (int)textureWidth);
    }
    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);

        float drawX = hitbox.x + (hitboxWidth - textureWidth) / 2f;
        float drawY = hitbox.y + (hitboxHeight - textureHeight) / 2f;


        batch.draw(texture, drawX, drawY, textureWidth, textureHeight);
        this.weapon.render(batch);
    }

    @Override
    public void update(float delta) {
        //player does not exceed the border of the map
        hitbox.setX(MathUtils.clamp(hitbox.getX(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET));
        hitbox.setY(MathUtils.clamp(hitbox.getY(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET));
    }

    public void handleInput(float delta) {
        if (Gdx.input.isTouched(Input.Buttons.LEFT)) {
            // changing input cords to world cords
            Vector3 unprojectedCords = game.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
            Vector2 direction = new Vector2(unprojectedCords.x - getCenterX(), unprojectedCords.y - getCenterY()).nor();

            weapon.shoot(direction);
        }

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
