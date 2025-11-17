package io.github.arenaShooter;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public abstract class Entity {
    protected Vector2 position;
    protected float width;
    protected float height;
    protected float health;

    public abstract void render(SpriteBatch batch);
    public abstract void update(float delta);
    public abstract void dispose();
    public Vector2 getPosition() { return position; }
    public Vector2 getMiddlePosition() { return new Vector2(position.x + width/2, position.y + height/2); }
    public float getHealth() { return width; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

}
