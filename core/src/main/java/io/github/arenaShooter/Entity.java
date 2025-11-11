package io.github.arenaShooter;

import com.badlogic.gdx.math.Vector2;

public interface Entity {
    protected Vector2 position;
    protected float health;



    public void render();
    public void update(float delta);
    public void dispose();
}
