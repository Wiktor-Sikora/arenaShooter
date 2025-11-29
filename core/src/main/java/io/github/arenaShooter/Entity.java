package io.github.arenaShooter;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import io.github.arenaShooter.ui.HealthBar;


public abstract class Entity {
    public Rectangle hitbox;
    protected boolean flipped = false;
    protected Main game;
    protected HealthBar healthBar;

    // stats
    public float range;
    public float rateOfFire;
    public float speed;
    public float maxHealth;
    public float health;

    public void takeDamage(int amount) {
        if (health <= 0) return;

        health -= amount;
        System.out.println("Enemy HP: " + health);

        if (health <= 0) {
            health = 0;
            kill();
        }
    }

    public void render(SpriteBatch batch) {
        healthBar.render(health, hitbox.x, hitbox.y);
    };

    public abstract void update(float delta);
    public void dispose() {
        healthBar.dispose();
    };

    public abstract void kill();

    public float getCenterX() { return hitbox.x + hitbox.width / 2;}
    public float getCenterY() { return hitbox.y + hitbox.height / 2;}
}
