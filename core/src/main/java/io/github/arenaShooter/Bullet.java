package io.github.arenaShooter;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
    public Rectangle hitbox;
    private Vector2 velocity;
    private float rotation = 0f;
    private float rotationSpeed = 720f;
    private float lifetime = 3f;
    private float timeAlive = 0f;
    private Texture texture;
    private float speed = 200f;


    /**
     * Constructs a new Bullet instance with the specified parameters.
     *
     * @param startX The starting x-coordinate of the bullet's position.
     * @param startY The starting y-coordinate of the bullet's position.
     * @param direction The direction vector in which the bullet should travel.
     * @param texture The texture representing the bullet.
     * @param speed The speed at which the bullet moves.
     * @param rotationSpeed The rate at which the bullet rotates, in degrees per second.
     * @param lifetime The lifetime of the bullet in seconds, determining when it expires.
     */
    public Bullet(float startX, float startY, Vector2 direction, Texture texture, float speed, float rotationSpeed, float lifetime) {
        this.hitbox = new Rectangle(startX, startY, 16, 16);

        this.texture = texture;
        this.speed = speed;
        this.velocity = new Vector2(direction).scl(speed); // transforms direction vector to velocity vector
        this.rotationSpeed = rotationSpeed;
        this.lifetime = lifetime;
    }

    public void update(float delta) {
        //update position
        hitbox.setX(hitbox.getX() + velocity.x * delta);
        hitbox.setY(hitbox.getY() + velocity.y * delta);

        //update rotation
        rotation += rotationSpeed * delta;
        if (rotation >= 360f) {
            rotation -= 360f;
        }

        timeAlive += delta;
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
        return timeAlive >= lifetime;
    }

    public Vector2 getVelocity() { return new Vector2(velocity); }
    public float getRotation() { return rotation; }

    public void setLifetime(float lifetime) { this.lifetime = lifetime; }
    public void setRotationSpeed(float rotationSpeed) { this.rotationSpeed = rotationSpeed; }
    public float getCenterX() { return hitbox.x + hitbox.width / 2;}
    public float getCenterY() { return hitbox.y + hitbox.height / 2;}

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}

