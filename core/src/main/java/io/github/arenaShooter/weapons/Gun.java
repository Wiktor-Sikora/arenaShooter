package io.github.arenaShooter.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

public class Gun extends Weapon {
    public Gun(Main game) {
        this.damage = 20;
        this.range = 300f;
        this.rateOfFire = 3f;
        this.projectileSpeed = 500f;
        this.weaponTexture = new Texture("gun.png");
        this.projectileTexture = new Texture("bullet.png");
        this.textureWidth = 20;
        this.textureHeight = 17;
        this.projectileOffsetX = 27;
        this.projectileOffsetY = 20;
        this.game = game;
        this.timeSinceLastShot = TimeUtils.millis();
    }
}
