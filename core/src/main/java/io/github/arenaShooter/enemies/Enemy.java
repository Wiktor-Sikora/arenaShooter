package io.github.arenaShooter.enemies;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.arenaShooter.Entity;

public abstract class Enemy extends Entity {
    public enum State {
        WALK,
        ATTACK,
        IDLE,
        DEAD,
        CHARGING,
        ALL_ACTIONS_FINISHED
    }
    protected State state = State.WALK;

    protected Animation<TextureRegion> walkAnimation;
    protected Animation<TextureRegion> attackAnimation;
    protected Animation<TextureRegion> deathAnimation;
    protected Sound deathSound;
    protected float stateTime = 0f;

    public boolean isRemote = false;

    // stats
    protected float projectileRange;
    protected float projectileSpeed;
    public float baseDamage;
    public float damage;

    @Override
    public void render(SpriteBatch batch) {
        if (isAlive()) {
            super.render(batch);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public boolean checkPlayerCollision() {
        return game.isAnyPlayerOverlapping(this.hitbox);
    }
    public boolean isDeathAnimationFinished() {
        return stateTime > deathAnimation.getAnimationDuration();
    }
    public boolean isDisposable() {
        return State.ALL_ACTIONS_FINISHED == state;
    }
    public boolean isAlive() { return State.DEAD != state; }

    public void kill() {
        healthBar.dispose();
        state = State.DEAD;
    }

    public void setDeadState() {
        healthBar.dispose();
        state = State.DEAD;
        stateTime = 0f;
    }

    public void stepTowardsPlayer(float delta, float dx, float dy, float distanceToPlayer) {
        if (isRemote) {
            return;
        }
        float newX = hitbox.x + (dx / distanceToPlayer) * speed * delta;
        float newY = hitbox.y + (dy / distanceToPlayer) * speed * delta;

        if (canMove(newX, newY, game.enemies)) {
            hitbox.x = newX;
            hitbox.y = newY;
        } else {
            float sideStep = speed * delta * 0.5f;
            if (canMove(hitbox.x, hitbox.y + sideStep, game.enemies)) hitbox.y += sideStep;
            else if (canMove(hitbox.x, hitbox.y - sideStep, game.enemies)) hitbox.y -= sideStep;
            else if (canMove(hitbox.x + sideStep, hitbox.y, game.enemies)) hitbox.x += sideStep;
        }
    }
}
