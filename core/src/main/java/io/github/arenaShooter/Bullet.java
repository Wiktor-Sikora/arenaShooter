package io.github.arenaShooter;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
    private float x, y;
    private Vector2 velocity;
    private float rotation = 0f;
    private float rotationSpeed = 720f;
    private float lifetime = 3f;
    private float timeAlive = 0f;
    private Texture texture;
    private float speed = 200f;

    public Bullet(float startX, float startY, Vector2 direction) {
        this.x = startX;
        this.y = startY;
        this.velocity = new Vector2(direction).scl(speed);
        this.texture = new Texture("bone.png");
    }

    public Bullet(float startX, float startY, Vector2 direction, float speed, float rotationSpeed) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;
        this.rotationSpeed = rotationSpeed;
        this.velocity = new Vector2(direction).scl(speed);
        this.texture = new Texture("bone.png");
    }

    public void update(float delta) {
        //update position
        x += velocity.x * delta;
        y += velocity.y * delta;

        //update rotation
        rotation += rotationSpeed * delta;
        if (rotation >= 360f) {
            rotation -= 360f;
        }

        timeAlive += delta;
    }

    public void render(SpriteBatch batch) {
        if (texture == null) return;


        float width = texture.getWidth();
        float height = texture.getHeight();

        batch.draw(texture,
            x - width/2, y - height/2,      //position (centre)
            width/2, height/2,             //pivot point (centre)
            width, height,                        //size
            1f, 1f,                        //scale
            rotation,                             //rotation
            0, 0,                            //texture region
            texture.getWidth(), texture.getHeight(),
            false, false);              //flip
    }

    public boolean isExpired() {
        return timeAlive >= lifetime;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return texture != null ? texture.getWidth() : 0; }
    public float getHeight() { return texture != null ? texture.getHeight() : 0; }
    public Vector2 getPosition() { return new Vector2(x, y); }
    public Vector2 getVelocity() { return new Vector2(velocity); }
    public float getRotation() { return rotation; }

    public void setLifetime(float lifetime) { this.lifetime = lifetime; }
    public void setRotationSpeed(float rotationSpeed) { this.rotationSpeed = rotationSpeed; }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}

