package io.github.arenaShooter.weapons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

public abstract class Weapon {
    // texture data
    protected float textureWidth;
    protected float textureHeight;
    protected Texture projectileTexture;
    protected Texture weaponTexture;
    protected float projectileOffsetX;
    protected float projectileOffsetY;

    // stats
    public String name;
    public int price;

    public float damage;
    public float range;
    public float rateOfFire; // bullets per second
    public float projectileSpeed;

    protected Main game;
    long timeSinceLastShot;

    public void shoot(Vector2 direction) {
        if (TimeUtils.timeSinceMillis(timeSinceLastShot) < 1000 / rateOfFire) return;

        this.timeSinceLastShot = TimeUtils.millis();

        spawnBullets(direction);

    }

    protected abstract void spawnBullets(Vector2 direction);

    public void render(SpriteBatch batch) {
        Vector3 unprojectedCords = game.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        Vector2 direction = new Vector2(unprojectedCords.x - game.player.getCenterX(), unprojectedCords.y - game.player.getCenterY()).nor();

        float rotation = direction.angleDeg();
        boolean flipped = false;
        float offsetX;
        if (direction.x < 0) {
            flipped = true;
            rotation = rotation + 180;
            offsetX = -30;
        } else {
            offsetX = 0;
        }


        batch.draw(weaponTexture,
            game.player.getCenterX() + direction.x * 15 + offsetX, game.player.getCenterY() + direction.y * 30,
            textureWidth/2, textureHeight/2,
            textureWidth, textureHeight,
            1f, 1f,
            rotation,
            0, 0,
            weaponTexture.getWidth(), weaponTexture.getHeight(),
            flipped, false
        );
    };

    public void dispose() {
        projectileTexture.dispose();
        weaponTexture.dispose();
    }
}
