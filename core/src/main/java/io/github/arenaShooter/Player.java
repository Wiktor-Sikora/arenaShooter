package io.github.arenaShooter;

import com.badlogic.gdx.graphics.Texture;

public class Player {
    public float x, y;
    public float speed;
    public boolean alive = true;
    public Texture texture;

    public Player(float startX, float startY, Texture texture) {
        this.x = startX;
        this.y = startY;
        this.speed = 250f;
        this.texture = texture;
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
