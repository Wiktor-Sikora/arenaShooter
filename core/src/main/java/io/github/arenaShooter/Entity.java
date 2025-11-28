package io.github.arenaShooter;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;

public abstract class Entity {
    public Rectangle hitbox;
    protected float health;
    protected float speed;
    protected float rateOfFire;
    protected float range;
    protected boolean flipped = false;
    protected Main game;

    public abstract void render(SpriteBatch batch);
    public abstract void update(float delta);
    public abstract void dispose();

    public float getCenterX() { return hitbox.x + hitbox.width / 2;}
    public float getCenterY() { return hitbox.y + hitbox.height / 2;}
}
