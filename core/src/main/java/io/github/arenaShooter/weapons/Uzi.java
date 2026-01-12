package io.github.arenaShooter.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

public class Uzi extends Weapon {
    public Uzi(Main game) {
        this.game = game;
        this.name = "Uzi";
        this.price = 200;

        this.damage = 20;
        this.range = 500f;
        this.rateOfFire = 5f; // NOWE: Szybsze strzelanie dla Uzi
        this.projectileSpeed = 500f;

        this.weaponTexture = new Texture("uzi_icon.png");
        this.projectileTexture = new Texture("bullet.png");
        this.textureWidth = 27;
        this.textureHeight = 23;
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
