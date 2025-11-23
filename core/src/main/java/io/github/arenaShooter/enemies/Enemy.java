package io.github.arenaShooter.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
import io.github.arenaShooter.Bullet;
import io.github.arenaShooter.Entity;
import io.github.arenaShooter.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class Enemy extends Entity {
    public enum State {
        WALK,
        ATTACK,
        IDLE,
        DEAD,
        ALL_ACTIONS_FINISHED
    }
    protected State state = State.WALK;

    protected Animation<TextureRegion> walkAnimation;
    protected Animation<TextureRegion> attackAnimation;
    protected Animation<TextureRegion> deathAnimation;
    protected Sound deathSound;
    protected float stateTime = 0f;

    protected float projectileRange;
    protected float projectileSpeed;

    public boolean checkPlayerCollision() {
        return this.hitbox.overlaps(game.player.hitbox);
    }

    public void takeDamage(int amount) {
        if (!isAlive()) return;

        health -= amount;
        System.out.println("Enemy HP: " + health);

        if (health <= 0) {
            health = 0;
            kill();
        }
    }

    public abstract void kill();
    public boolean isDeathAnimationFinished() {
        return stateTime > deathAnimation.getAnimationDuration();
    }
    public boolean isDisposable() {
        return State.ALL_ACTIONS_FINISHED == state;
    }
    public boolean isAlive() { return State.DEAD != state; }
}

