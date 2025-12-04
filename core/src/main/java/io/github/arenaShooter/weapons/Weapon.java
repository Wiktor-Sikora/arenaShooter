package io.github.arenaShooter.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

public abstract class Weapon {
    // texture data
    protected float textureWidth;
    protected float textureHeight;
    protected Texture projectileTexture;
    protected Texture weaponTexture;

    // stats
    public float damage;
    public float range;
    public float rateOfFire; // bullets per second
    public float projectileSpeed;

    protected Main game;
    long timeSinceLastShot;

    public void shoot(Vector2 direction) {
        if (TimeUtils.timeSinceMillis(timeSinceLastShot) < 1000 / rateOfFire) return;

        this.timeSinceLastShot = TimeUtils.millis();

        game.addBullet(new Bullet(this.game,
            game.player.getCenterX(), game.player.getCenterY(),
            direction, projectileTexture, 5, 15,
            this.damage, this.projectileSpeed, this.range,
            Bullet.Owner.PLAYER
        ));
    }

    public void render(SpriteBatch batch) {
        batch.draw(
            weaponTexture,
            game.player.getCenterX() - textureWidth / 2 + 30,
            game.player.getCenterY() - textureHeight / 2,
            textureWidth, textureHeight
        );
    };

    public void dispose() {
        projectileTexture.dispose();
        weaponTexture.dispose();
    }
}
