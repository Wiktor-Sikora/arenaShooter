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
    protected int playerId = -1;

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    protected int getPlayerId() {
        return playerId;
    }

    public void shoot(Vector2 direction) {
        if (TimeUtils.timeSinceMillis(timeSinceLastShot) < 1000 / rateOfFire) return;

        this.timeSinceLastShot = TimeUtils.millis();

        direction.nor();

        float rotation;
        boolean flipped = false;

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            if (direction.x > 0) {
                rotation = 0;
                flipped = false;
            } else {
                rotation = 0;
                flipped = true;
            }
        } else {
            if (direction.y > 0) rotation = 90;
            else rotation = -90;
        }

        float offsetX, offsetY;

        if (rotation == 0) {
            offsetX = flipped ? -35 : 15;
            offsetY = 0;
        } else if (rotation == 90) {
            offsetX = -10;
            offsetY = 25;
        } else { // -90
            offsetX = 10;
            offsetY = -15;
        }

        float weaponX = game.player.getCenterX() + offsetX;
        float weaponY = game.player.getCenterY() + offsetY;

        float bulletX = weaponX;
        float bulletY = weaponY;

        if (rotation == 0 && !flipped) {         //right
            bulletX += textureWidth / 2f + 10;
        }
        else if (rotation == 0 && flipped) {     //left
            bulletX -= textureWidth / 2f + 10;
        }
        else if (rotation == 90) {               //up
            bulletY += textureHeight / 2f + 10;
        }
        else if (rotation == -90) {              //down
            bulletY -= textureHeight / 2f + 10;
        }

        game.addBullet(new Bullet(
            this.game,
            bulletX, bulletY,
            direction,
            projectileTexture,
            5, 15,
            this.damage,
            this.projectileSpeed,
            this.range,
            Bullet.Owner.PLAYER
        ), getPlayerId());
    }

    public void render(SpriteBatch batch) {
        Vector3 unprojectedCords = game.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        Vector2 direction = new Vector2(unprojectedCords.x - game.player.getCenterX(), unprojectedCords.y - game.player.getCenterY()).nor();

        float rotation = 0f;
        boolean flipped = false;

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            if (direction.x > 0) rotation = 0;
            else { rotation = 0; flipped = true; }
        } else { //up / down
            if (direction.y > 0) rotation = 90;
            else rotation = -90;
        }

        float offsetX = 0, offsetY = 0;
        if (game.player.facingLeft) {
            offsetX = -35;
        } else {
            offsetX = 15;
        }

        if (rotation == 90) {
            offsetX = -10;
            offsetY = 25;
        }
        else if (rotation == -90) {
            offsetX = 0;
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
