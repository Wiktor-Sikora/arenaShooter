package io.github.arenaShooter.weapons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

public class Uzi extends Weapon {
    public Uzi(Main game) {
        this.name = "Uzi";
        this.price = 200;
        this.damage = 20;
        this.range = 500;
        this.rateOfFire = 5f;
        this.projectileSpeed = 500f;
        this.weaponTexture = new Texture("uzi_icon.png");
        this.projectileTexture = new Texture("bullet.png");
        this.textureWidth = 20;
        this.textureHeight = 17;
        this.projectileOffsetX = 27;
        this.projectileOffsetY = 20;
        this.game = game;
        this.timeSinceLastShot = TimeUtils.millis();
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
            offsetX = -20;
            offsetY = -5;
        } else {
            offsetX = 0;
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
