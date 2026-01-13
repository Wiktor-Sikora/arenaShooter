package io.github.arenaShooter.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
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
}
