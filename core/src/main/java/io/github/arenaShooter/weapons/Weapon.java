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
    public float damage;
    public float range;
    public float rateOfFire; // bullets per second
    public float projectileSpeed;

    protected Main game;
    long timeSinceLastShot;

    public void shoot(Vector2 direction) {
        if (TimeUtils.timeSinceMillis(timeSinceLastShot) < 1000 / rateOfFire) return;

        this.timeSinceLastShot = TimeUtils.millis();

        // TODO: add bullet offset to the end of a barrel
        // TODO: add bullet initial rotation
//        game.addBullet(new Bullet(this.game,
//            game.player.getCenterX(), game.player.getCenterY(),
//            direction, projectileTexture, 5, 15,
//            this.damage, this.projectileSpeed, this.range,
//            Bullet.Owner.PLAYER
//        ));

        // Obliczamy offset od środka gracza w zależności od kierunku (4 kierunki)
        float offsetX = 0;
        float offsetY = 0;
        if (Math.abs(direction.x) > Math.abs(direction.y)) { // lewo/prawo
            offsetX = direction.x > 0 ? 20 : -20;
            offsetY = 0;
        } else { // góra/dół
            offsetX = 0;
            offsetY = direction.y > 0 ? 20 : -20;
        }

        game.addBullet(new Bullet(this.game,
            game.player.getCenterX() + offsetX, game.player.getCenterY() + offsetY,
            direction, projectileTexture, 5, 15,
            this.damage, this.projectileSpeed, this.range,
            Bullet.Owner.PLAYER
        ));
    }

    public void render(SpriteBatch batch) {
        Vector3 unprojectedCords = game.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        Vector2 direction = new Vector2(unprojectedCords.x - game.player.getCenterX(), unprojectedCords.y - game.player.getCenterY()).nor();

//        float rotation = direction.angleDeg();
//        boolean flipped = false;
//        float offsetX;
//        if (direction.x < 0) {
//            flipped = true;
//            rotation = rotation + 180;
//            offsetX = -30;
//        } else {
//            offsetX = 0;
//        }
//
//
//        batch.draw(weaponTexture,
//            game.player.getCenterX() + direction.x * 15 + offsetX, game.player.getCenterY() + direction.y * 30,
//            textureWidth/2, textureHeight/2,
//            textureWidth, textureHeight,
//            1f, 1f,
//            rotation,
//            0, 0,
//            weaponTexture.getWidth(), weaponTexture.getHeight(),
//            flipped, false
//        );


        float rotation = 0f;
        boolean flipped = false;

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            if (direction.x > 0) rotation = 0;
            else { rotation = 0; flipped = true; }
        } else { // góra/dół
            if (direction.y > 0) rotation = 90;
            else rotation = -90;
        }

        float offsetX = 0, offsetY = 0;
        if (game.player.facingLeft) {
            offsetX = -35;
        } else {
            offsetX = 35;
        }


        if (rotation == 90) {
            offsetX = -40;
            offsetY = 15;
        }
        else if (rotation == -90) {
            offsetX = 10;
            offsetY = -15;
        }

        batch.draw(
            weaponTexture,
            game.player.getCenterX() + offsetX,
            game.player.getCenterY() + offsetY,
            textureWidth / 2, textureHeight / 2,
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
