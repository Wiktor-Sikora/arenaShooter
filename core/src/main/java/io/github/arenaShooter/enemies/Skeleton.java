package io.github.arenaShooter.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.arenaShooter.weapons.Bullet;
import io.github.arenaShooter.Main;
import io.github.arenaShooter.ui.HealthBar;

public class Skeleton extends Enemy {
    private int attackCount = 0;
    private float restTime = 0f;
    private boolean hasShotThisCycle = false;

    private float damageTimer = 0f;

    private final float REST_DURATION = 1f;
    private final int MAX_ATTACKS = 3;
    private final Texture bulletTexture = new Texture("bone.png");
    private final TextureAtlas textureAtlas = new TextureAtlas(Gdx.files.internal("skeleton.atlas"));
    private final TextureAtlas deathAnimationAtlas = new TextureAtlas(Gdx.files.internal("death.atlas"));
    private final Sound deathSound = Gdx.audio.newSound(Gdx.files.internal("death_sound.mp3"));

    public Skeleton(float startX, float startY, Main game) {
        this.textureHeight = textureWidth = 64;
        this.hitboxHeight = 27;
        this.hitboxWidth = textureWidth / 2;
        this.hitbox = new Rectangle(startX, startY, hitboxWidth, hitboxHeight);
        this.baseDamage = 10f;
        this.damage = 10f;
        this.projectileSpeed = 100f;
        this.projectileRange = 300f;
        this.baseSpeed = 100f;
        this.speed = 100f;
        this.maxHealth = this.health = 100;
        this.range = 150f;
        this.rateOfFire = 0.5f;
        this.game = game;

        Array<TextureRegion> walkFrames = new Array<>();
        for (int i = 0; i < 3; i++) {
            walkFrames.add(textureAtlas.findRegion("skeleton_walk_" + i));
        }
        walkAnimation = new Animation<>(0.15f, walkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> attackFrames = new Array<>();
        for (int i = 0; i < 2; i++) {
            attackFrames.add(textureAtlas.findRegion("skeleton_attack_" + i));
        }
        attackAnimation = new Animation<>(0.15f, attackFrames, Animation.PlayMode.NORMAL);

        Array<TextureRegion> deathFrames = new Array<>();
        for (int i = 0; i < 47; i++) {
            deathFrames.add(deathAnimationAtlas.findRegion("death_animation" + i));
        }
        deathAnimation = new Animation<>(0.05f, deathFrames, Animation.PlayMode.NORMAL);

        this.healthBar = new HealthBar(game, maxHealth, (int)textureWidth);
    }

    @Override
    public void update(float delta) {
        stateTime += delta;
        damageTimer += delta;

        float dx = game.player.hitbox.getX() - hitbox.getX();
        float dy = game.player.hitbox.getY() - hitbox.getY();
        float distanceToPlayer = (float) Math.sqrt(dx * dx + dy * dy);

        flipped = (game.player.hitbox.getX() < hitbox.getX());

        switch (state) {
            case WALK:
                //go to player
                if (distanceToPlayer > range) {
                    stepTowardsPlayer(delta, dx, dy, distanceToPlayer);
                } else {
                    state = State.ATTACK;
                    stateTime = 0f;
                    hasShotThisCycle = false;
                }
                break;

            case ATTACK:
                //onetime animation
                if(!hasShotThisCycle && stateTime >= attackAnimation.getFrameDuration()) {
                    shoot(game.player.getCenterX(), game.player.getCenterY());
                    attackCount++;
                    hasShotThisCycle = true;
                }

                if(attackAnimation.isAnimationFinished(stateTime)) {
                    if(attackCount >= MAX_ATTACKS) {
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
                    state = State.ALL_ACTIONS_FINISHED;
                }
                break;
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);

        TextureRegion currentFrame;

        switch (state) {
            case ATTACK:
                currentFrame = attackAnimation.getKeyFrame(stateTime, false);
                break;
            case WALK:
                currentFrame = walkAnimation.getKeyFrame(stateTime, true);
                break;
            case DEAD:
                currentFrame = deathAnimation.getKeyFrame(stateTime, false);
                if (currentFrame != null) {
                    TextureRegion toDraw = new TextureRegion(currentFrame);

                    float drawX = hitbox.x + (hitboxWidth - textureWidth) / 2f;
                    float drawY = hitbox.y + (hitboxHeight - textureHeight) / 2f;
                    batch.draw(toDraw, drawX, drawY, textureWidth, textureHeight);
                }
                return;
            case ALL_ACTIONS_FINISHED:
                return;
            default:
                currentFrame = walkAnimation.getKeyFrame(0, false);
                break;
        }

        // create a copy to flip
        TextureRegion frameToDraw = new TextureRegion(currentFrame);
        if (flipped) {
            frameToDraw.flip(true, false);
        }

        float drawX = hitbox.x + (hitboxWidth - textureWidth) / 2f;
        float drawY = hitbox.y + (hitboxHeight - textureHeight) / 2f;

        batch.draw(frameToDraw, drawX, drawY, textureWidth, textureHeight);
    }

    @Override
    protected float getHealthBarOffsetY() {
        return textureHeight / 4f;
    }

    @Override
    public void dispose() {
        super.dispose();

        if (deathSound != null) {
            deathSound.dispose();
        }
    }

    @Override
    public void kill() {
        super.kill();

        deathSound.play();
        stateTime = 0f;
    }

    private void shoot(float targetX, float targetY) {
        Vector2 direction = new Vector2(targetX - getCenterX(), targetY - getCenterY()).nor();

        game.addBullet(new Bullet(this.game,
            getCenterX(), getCenterY(), direction,
            bulletTexture, 16, 16,
            this.damage, this.projectileSpeed, this.projectileRange,
            Bullet.Owner.ENEMY, 720f
        ));
    }


}
