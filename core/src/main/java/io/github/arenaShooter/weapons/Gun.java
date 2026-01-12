package io.github.arenaShooter.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

public class Gun extends Weapon {
    public Gun(Main game) {
        this.name = "Pistolet";
        this.price = 0;
        this.damage = 20;
        this.range = 500f;
        this.rateOfFire = 3f;
        this.projectileSpeed = 500f;
        this.weaponTexture = new Texture("gun.png");
        this.projectileTexture = new Texture("bullet.png");
        this.textureWidth = 27;
        this.textureHeight = 23;
        this.projectileOffsetX = 27;
        this.projectileOffsetY = 20;
        this.game = game;
        this.timeSinceLastShot = TimeUtils.millis();
    }

    @Override
    protected void spawnBullets(Vector2 direction) {

        // TODO: add bullet offset to the end of a barrel
        // TODO: add bullet initial rotation
        game.addBullet(new Bullet(this.game,
            game.player.getCenterX(), game.player.getCenterY(),
            direction, projectileTexture, 5, 15,
            this.damage, this.projectileSpeed, this.range,
            Bullet.Owner.PLAYER
        ));
    }
}
