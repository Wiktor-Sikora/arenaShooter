package io.github.arenaShooter.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
import io.github.arenaShooter.Bullet;
import io.github.arenaShooter.Entity;
import io.github.arenaShooter.Player;

import java.util.ArrayList;
import java.util.List;

public class Enemy extends Entity {
    //enum states
    public enum State {
        WALK,
        ATTACK,
        IDLE,
        DEAD
    }

    private float speed = 100f;
    private float attackRange = 150f;
    private boolean alive = true;

    private State state = State.WALK;

    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> attackAnimation;
    private Animation<TextureRegion> deathAnimation;
    private Sound deathSound;
    private float stateTime = 0f;
    private boolean flipped = false;

    private int health = 100;
    private final int DAMAGE_ON_CONTACT = 50;
    private float damageCooldown = 1f;
    private float damageTimer = 0f;

    private int attackCount = 0;
    private final int maxAttacks = 3;
    private float restTime = 0f;
    private final float REST_DURATION = 1f;
    private boolean hasShotThisCycle = false;

    private boolean deathAnimationFinished = false;
    private List<Bullet> bullets = new ArrayList<>();

    private Player target;

    public Enemy(float startX, float startY, TextureAtlas atlasEnemy, TextureAtlas atlasDeath, Player target) {
        this.position = new Vector2(startX, startY);
        this.width = 64;
        this.height = 64;
        this.target = target;

        Array<TextureRegion> walkFrames = new Array<>();
        for (int i = 0; i < 3; i++) {
            walkFrames.add(atlasEnemy.findRegion("skeleton_walk_" + i));
        }
        walkAnimation = new Animation<>(0.15f, walkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> attackFrames = new Array<>();
        for (int i = 0; i < 2; i++) {
            attackFrames.add(atlasEnemy.findRegion("skeleton_attack_" + i));
        }

        attackAnimation = new Animation<>(0.15f, attackFrames, Animation.PlayMode.NORMAL);

        Array<TextureRegion> deathFrames = new Array<>();
        for (int i = 0; i < 47; i++) {
            deathFrames.add(atlasDeath.findRegion("death_animation" + i));
        }
        deathAnimation = new Animation<>(0.05f, deathFrames, Animation.PlayMode.NORMAL);

        deathSound = Gdx.audio.newSound(Gdx.files.internal("death_sound.mp3"));
    }

    @Override
    public void update(float delta) {
        if (state == State.DEAD) {
            stateTime += delta;
            if (deathAnimation.isAnimationFinished(stateTime)) {
                alive = false;
                deathAnimationFinished = true;
            }
            return;
        }

        stateTime += delta;
        damageTimer += delta;

        float dx = target.getPosition().x - position.x;
        float dy = target.getPosition().y - position.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        flipped = (target.getPosition().x < position.x);

        switch (state) {
            case WALK:
                //go to player
                if (dist > attackRange) {
                    position.x += (dx / dist) * speed * delta;
                    position.y += (dy / dist) * speed * delta;
                } else {
                    state = State.ATTACK;
                    stateTime = 0f;
                    hasShotThisCycle = false;
                }
                break;

            case ATTACK:
                //onetime animation
                if (!hasShotThisCycle && stateTime >= attackAnimation.getFrameDuration()) {
                    shoot(target.getMiddlePosition().x, target.getMiddlePosition().y);
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
                if (deathAnimation.isAnimationFinished(stateTime)) {
                    deathAnimationFinished = true;
                }
                break;
        }

        checkPlayerCollision();

        // bullets update
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(delta);

            // delete if are out of map
            if (bullet.isExpired()) {
                bullets.remove(i);
            }
        }

    }

    // fix this method to avoid creating new objects
    private void checkPlayerCollision() {
        //collision rectangle for enemy and player
        Rectangle enemyRect = new Rectangle(position.x, position.y, 64, 64);
        Rectangle playerRect = new Rectangle(target.getMiddlePosition().x, target.getMiddlePosition().y, 64, 64);

        if (enemyRect.overlaps(playerRect) && damageTimer >= damageCooldown) {
            takeDamage(DAMAGE_ON_CONTACT);
            damageTimer = 0f;
        }
    }

    public void takeDamage(int amount) {
        if (state == State.DEAD) return;
        health -= amount;
        System.out.println("Enemy HP: " + health);

        if (health <= 0) {
            health = 0;
            kill();
        }
    }

    //shoot
    private void shoot(float targetX, float targetY) {
        //shoot direction
        Vector2 direction = new Vector2(targetX - getMiddlePosition().x, targetY - getMiddlePosition().y).nor();

        //create new bullet
        Bullet bullet = new Bullet(getMiddlePosition().x, getMiddlePosition().y, direction);
        bullets.add(bullet);
    }

    @Override
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
            case DEAD:
                TextureRegion deathFrame = deathAnimation.getKeyFrame(stateTime, false);
                if (deathFrame != null) {
                    TextureRegion toDraw = new TextureRegion(deathFrame);
                    if (flipped) toDraw.flip(true, false);
                    batch.draw(toDraw, position.x, position.y, 72, 72);
                }
                return;
            case IDLE:
            default:
                currentFrame = walkAnimation.getKeyFrame(0, false);
                break;
        }

        // create a copy to flip
        TextureRegion frameToDraw = new TextureRegion(currentFrame);
        if (flipped) {
            frameToDraw.flip(true, false);
        }

        batch.draw(frameToDraw, position.x, position.y, 64, 64);

        //draw all bullets
        for (Bullet bullet : bullets) {
            bullet.render(batch);
        }
    }


    @Override
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
        if (state == State.DEAD) return;
        deathSound.play();
        state = State.DEAD;
        stateTime = 0f;
    }

    public boolean isDeathAnimationFinished() {
        return stateTime > deathAnimation.getAnimationDuration();
    }

    public boolean isAlive() { return alive; }
}

