package io.github.arenaShooter.weapons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

import java.util.Random;

public class Shotgun extends Weapon {
    int bulletsPerShot = 6;

    public Shotgun(Main game) {
        this.name = "Shotgun";
        this.price = 200;
        this.damage = 8;  // per bullet
        this.range = 500f;
        this.rateOfFire = 1f;
        this.projectileSpeed = 500f;
        this.weaponTexture = new Texture("shotgun.png");
        this.projectileTexture = new Texture("bullet.png");
        this.textureWidth = 33;
        this.textureHeight = 14;
        this.projectileOffsetX = 27;
        this.projectileOffsetY = 20;
        this.game = game;
        this.timeSinceLastShot = TimeUtils.millis();
    }

    @Override
    public void shoot(Vector2 direction) {
        if (TimeUtils.timeSinceMillis(timeSinceLastShot) < 800 / rateOfFire) return;
        this.timeSinceLastShot = TimeUtils.millis();

        Random random = new Random();
        direction.nor();
        float rotation;
        boolean flipped = false;

        for (int i = 0; i < bulletsPerShot; i++) {
            Vector2 newDirection = new Vector2(direction.x + (random.nextFloat() * 0.3f) - 0.15f, direction.y + (random.nextFloat() * 0.3f) - 0.15f);

            if (Math.abs(newDirection.x) > Math.abs(newDirection.y)) {
                if (newDirection.x > 0) {
                    rotation = 0;
                    flipped = false;
                } else {
                    rotation = 0;
                    flipped = true;
                }
            } else {
                if (newDirection.y > 0) rotation = 90;
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
                newDirection,
                projectileTexture,
                5, 15,
                this.damage,
                this.projectileSpeed,
                this.range,
                Bullet.Owner.PLAYER
            ));
        }
    }

    @Override
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
            offsetX = -28;
            offsetY = -5;
        } else {
            offsetX = -3;
            offsetY = -5;
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
}
