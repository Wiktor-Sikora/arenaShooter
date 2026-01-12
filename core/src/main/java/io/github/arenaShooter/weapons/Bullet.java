package io.github.arenaShooter.weapons;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.arenaShooter.Main;
import io.github.arenaShooter.enemies.Enemy;

/**
 * ============================================
 * ZAKTUALIZOWANA KLASA - Bullet (pocisk)
 * ============================================
 * BEZ ZMIAN - identyczna jak oryginał
 */
public class Bullet {
    public enum Owner {
        PLAYER,
        ENEMY
    }
    private Owner owner;

    public Rectangle hitbox;
    private Vector2 velocity;
    private Texture texture;
    private Main game;

    private boolean expired = false;
    private boolean isRotating = false;
    private float distanceTraveled = 0f;

    // stats
    private float damage;
    private float speed = 200f;
    private float range;
    private float rotationSpeed;
    private float rotation = 0f;

    public Bullet(Main game, float startX, float startY, Vector2 direction, Texture texture, int width, int height, float damage, float speed, float range, Owner owner, float rotationSpeed) {
        this.hitbox = new Rectangle(startX, startY, width, height);
        this.game = game;

        // stats
        this.damage = damage;
        this.speed = speed;
        this.range = range;

        this.texture = texture;
        this.velocity = new Vector2(direction).scl(speed);
        this.owner = owner;

        // rotation
        this.rotationSpeed = rotationSpeed;
        this.isRotating = true;
    }

    public Bullet(Main game, float startX, float startY, Vector2 direction, Texture texture, int width, int height, float damage, float speed, float range, Owner owner) {
        this.hitbox = new Rectangle(startX, startY, width, height);
        this.game = game;

        // stats
        this.damage = damage;
        this.speed = speed;
        this.range = range;

        this.texture = texture;
        this.velocity = new Vector2(direction).scl(speed);
        this.owner = owner;
    }

    public void update(float delta) {
        // position
        hitbox.setX(hitbox.getX() + velocity.x * delta);
        hitbox.setY(hitbox.getY() + velocity.y * delta);

        distanceTraveled += velocity.len() * delta;

        // collision detection
        if (owner == Owner.PLAYER) {
            for (Enemy target: this.game.enemies) {
                if (target.isAlive() && this.hitbox.overlaps(target.hitbox)) {
                    target.takeDamage(this.damage);
                    this.expired = true;
                    return;
                }
            }
        } else {
            if (this.hitbox.overlaps(this.game.player.hitbox)) {
                this.game.player.takeDamage(this.damage);
                this.expired = true;
                return;
            }
        }

        // rotation
        if (isRotating) {
            rotation += rotationSpeed * delta;
            if (rotation >= 360f) {
                rotation -= 360f;
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (texture == null) return;

        batch.draw(texture,
            getCenterX(), getCenterY(),
            hitbox.width/2, hitbox.height/2,
            hitbox.width, hitbox.height,
            1f, 1f,
            rotation,
            0, 0,
            texture.getWidth(), texture.getHeight(),
            false, false);
    }

    public boolean isExpired() {
        return expired || distanceTraveled >= range;
    }

    public Vector2 getVelocity() { return new Vector2(velocity); }
    public float getRotation() { return rotation; }

    public void setRotationSpeed(float rotationSpeed) { this.rotationSpeed = rotationSpeed; }
    public float getCenterX() { return hitbox.x + hitbox.width / 2;}
    public float getCenterY() { return hitbox.y + hitbox.height / 2;}

    public void dispose() {
        // Texture is shared, don't dispose here
    }
}
