package io.github.arenaShooter.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import io.github.arenaShooter.Main;

// NOWE: Klasa Shotgun
public class Shotgun extends Weapon {
    public Shotgun(Main game) {
        this.game = game;
        this.name = "Shotgun";
        this.price = 200; // Cena w sklepie

        this.damage = 15; // Mniejszy dmg per pocisk, ale jest ich dużo
        this.range = 350f; // Krótszy zasięg
        this.rateOfFire = 1.2f; // Wolniejsze strzelanie
        this.projectileSpeed = 600f;

        this.weaponTexture = new Texture("shotgun_icon.png"); // Placeholder (możesz dać inną grafikę)
        this.projectileTexture = new Texture("bullet.png"); // Placeholder
        this.textureWidth = 27;
        this.textureHeight = 23;
        this.timeSinceLastShot = TimeUtils.millis();
    }

    // NOWE: Implementacja strzału wielokrotnego (rozrzut)
    @Override
    protected void spawnBullets(Vector2 direction) {
        // Pętla tworząca 5 pocisków
        for (int i = -2; i <= 2; i++) {
            // Obracamy wektor kierunku o kilka stopni dla każdego pocisku
            Vector2 spreadDir = direction.cpy().rotateDeg(i * 10f);

            game.addBullet(new Bullet(this.game,
                game.player.getCenterX(), game.player.getCenterY(),
                spreadDir, projectileTexture, 5, 15,
                this.damage, this.projectileSpeed, this.range,
                Bullet.Owner.PLAYER
            ));
        }
    }
}
