package io.github.arenaShooter;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import io.github.arenaShooter.enemies.Enemy;
import io.github.arenaShooter.ui.HealthBar;


public abstract class Entity {
    public Rectangle hitbox;
    protected float hitboxWidth;
    protected float hitboxHeight;

    protected float textureWidth;
    protected float textureHeight;

    public boolean flipped = false;
    protected Main game;
    protected HealthBar healthBar;

    // stats
    public float range;
    public float rateOfFire;
    public float baseSpeed;
    public float speed;
    public float maxHealth;
    public float health;

    public void takeDamage(float amount) {
        if (health <= 0) return;
        health -= amount;

        if (health <= 0) {
            health = 0;
            kill();
        }
    }

    public boolean isCollidingWith(Entity other) {
        if (other == this) return false;
        return this.hitbox.overlaps(other.hitbox);
    }

    protected boolean canMove(float newX, float newY, Array<Enemy> enemies) {
        Rectangle futureHitbox = new Rectangle(newX, newY, hitboxWidth, hitboxHeight);
        for(Enemy e : enemies) {
            if(e == this) continue;
            if(e.isAlive() && futureHitbox.overlaps(e.hitbox)) return false;
        }
        return true;
    }

    protected float getHealthBarOffsetY() {
        return 0f;
    }

    public void render(SpriteBatch batch) {
        float healthBarX = hitbox.x + (hitboxWidth - textureWidth) / 2f;
        float healthBarY = hitbox.y - getHealthBarOffsetY();

        healthBar.render(health, healthBarX, healthBarY);
    };

    public abstract void update(float delta);
    public void dispose() {
        if (healthBar != null) healthBar.dispose();
    };

    public abstract void kill();

    public float getCenterX() { return hitbox.x + hitboxWidth / 2; }
    public float getCenterY() { return hitbox.y + hitboxHeight / 2; }
}
