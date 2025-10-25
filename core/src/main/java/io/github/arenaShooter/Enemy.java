package io.github.arenaShooter;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class Enemy {

    //enum states
    public enum State {
        WALK,
        ATTACK,
        IDLE,
        DEAD
    }

    private float x, y;
    private float speed = 100f;
    private float attackRange = 150f;
    private boolean alive = true;

    private State state = State.WALK;

    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> attackAnimation;
    private float stateTime = 0f;
    private boolean flipped = false;

    private int attackCount = 0;
    private final int maxAttacks = 3;
    private float restTime = 0f;
    private final float REST_DURATION = 1f;

    private boolean hasShotThisCycle = false;

    private List<Bullet> bullets = new ArrayList<>();

    public Enemy(float startX, float startY, TextureAtlas atlas) {
        this.x = startX;
        this.y = startY;

        Array<TextureRegion> walkFrames = new Array<>();
        walkFrames.add(atlas.findRegion("skeleton_walk0"));
        walkFrames.add(atlas.findRegion("skeleton_walk1"));
        walkFrames.add(atlas.findRegion("skeleton_walk2"));
        walkAnimation = new Animation<>(0.15f, walkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> attackFrames = new Array<>();
        attackFrames.add(atlas.findRegion("skeleton_attack1"));
        attackFrames.add(atlas.findRegion("skeleton_attack2"));
        attackAnimation = new Animation<>(0.15f, attackFrames, Animation.PlayMode.NORMAL);

    }

    //update state
    public void update(float delta, float playerX, float playerY) {
        if (!alive) return;

        stateTime += delta;

        float dx = playerX - x;
        float dy = playerY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        flipped = (playerX < x);

        switch (state) {
            case WALK:
                //go to player
                if (dist > attackRange) {
                    x += (dx / dist) * speed * delta;
                    y += (dy / dist) * speed * delta;
                } else {
                    state = State.ATTACK;
                    stateTime = 0f;
                    hasShotThisCycle = false;
                }
                break;

            case ATTACK:
                //onetime animation
                if (!hasShotThisCycle && stateTime >= attackAnimation.getFrameDuration()) {
                    shoot(playerX, playerY);
                    attackCount++;
                    hasShotThisCycle = true;
                }

                if (attackAnimation.isAnimationFinished(stateTime)) {
                    if (attackCount >= maxAttacks) {
                        // IDLE after 3 attacks
                        state = State.IDLE;
                        restTime = 0f;
                        stateTime = 0f;
                    } else {
                        state = State.WALK;
                        stateTime = 0f;
                    }
                }
                break;

            case IDLE:
                restTime += delta;
                if (restTime >= REST_DURATION) {
                    attackCount = 0;
                    restTime = 0f;
                    state = State.WALK;
                    stateTime = 0f;
                }
                break;

            case DEAD:
                //todo
                break;
        }

        //bullets update
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(delta);

            //delete if are out of map
            if (bullet.isExpired()) {
                bullets.remove(i);
            }
        }

    }

    //shoot
    private void shoot(float targetX, float targetY) {
        //shoot direction
        Vector2 direction = new Vector2(targetX - (x + 32), targetY - (y + 32)).nor();

        //create new bullet
        Bullet bullet = new Bullet(x + 32, y + 32, direction);
        bullets.add(bullet);
    }

    //drawing
    public void render(SpriteBatch batch) {
        if (!alive) return;

        TextureRegion currentFrame;
        switch (state) {
            case ATTACK:
                currentFrame = attackAnimation.getKeyFrame(stateTime, false);
                break;
            case WALK:
                currentFrame = walkAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE:
            default:
                currentFrame = walkAnimation.getKeyFrame(0, false);
                break;
        }

        //create copy to flip
        TextureRegion frameToDraw = new TextureRegion(currentFrame);
        if (flipped) {
            frameToDraw.flip(true, false);
        }

        batch.draw(frameToDraw, x, y, 64, 64);

        //draw all bullets
        for (Bullet bullet : bullets) {
            bullet.render(batch);
        }
    }

    //clear bullets
    public void dispose() {
        for (Bullet bullet : bullets) {
            bullet.dispose();
        }
        bullets.clear();
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public void kill() {
        alive = false;
        state = State.DEAD;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isAlive() { return alive; }
}

