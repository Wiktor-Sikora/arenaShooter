package io.github.arenaShooter.weapons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.arenaShooter.Entity;
import io.github.arenaShooter.Main;
import io.github.arenaShooter.enemies.Enemy;

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

    private Vector2 snapDirectionTo4(Vector2 dir) {
        if (Math.abs(dir.x) > Math.abs(dir.y)) {
            return new Vector2(dir.x > 0 ? 1 : -1, 0);
        } else {
            return new Vector2(0, dir.y > 0 ? 1 : -1);
        }
    }

    public Bullet(Main game, float startX, float startY, Vector2 direction, Texture texture, int width, int height, float damage, float speed, float range, Owner owner, float rotationSpeed) {
        this.hitbox = new Rectangle(startX, startY, width, height);
        this.game = game;

        // stats
        this.damage = damage;
        this.speed = speed;
        this.range = range;

        this.texture = texture;
        this.velocity = new Vector2(direction).scl(speed); // transforms direction vector to velocity vector
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

        this.owner = owner;

        Vector2 snapped = snapDirectionTo4(direction);
        this.velocity = new Vector2(direction).scl(speed); // transforms direction vector to velocity vector

        if (snapped.x == 1)        rotation = 270;      //right
        else if (snapped.x == -1) rotation = 90;     //left
        else if (snapped.y == 1)   rotation = 0;     //up
        else if (snapped.y == -1)  rotation = 180;    //down
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
            getCenterX(), getCenterY(),                 //position (centre)
            hitbox.width/2, hitbox.height/2,    //pivot point (centre)
            hitbox.width, hitbox.height,                //size
            1f, 1f,                             //scale
            rotation,                                   //rotation
            0, 0,                                 //texture region
            texture.getWidth(), texture.getHeight(),
            false, false);                   //flip
    }

public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    public Vector2 getVelocity() { return new Vector2(velocity); }
    public float getRotation() { return rotation; }
    public Owner getOwner() { return owner; }

    public void setRotationSpeed(float rotationSpeed) { this.rotationSpeed = rotationSpeed; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public void setVelocity(float vx, float vy) {
        this.velocity.set(vx, vy);
        this.rotation = (float)Math.toDegrees(Math.atan2(vy, vx)) + 90;
    }
    public void setPosition(float x, float y) { this.hitbox.setPosition(x, y); }
    public void setSize(float width, float height) { this.hitbox.setSize(width, height); }
    public float getCenterX() { return hitbox.x + hitbox.width / 2;}
    public float getCenterY() { return hitbox.y + hitbox.height / 2;}

    public float getDamage() { return damage; }

    public void hit(Enemy target) {
        target.takeDamage(damage);
        expired = true;
    }

    public void dispose() {
//        if (texture != null) {
//            texture.dispose();
//        }
    }
}
