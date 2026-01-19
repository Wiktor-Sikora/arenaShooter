package io.github.arenaShooter.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.arenaShooter.weapons.Bullet;
import io.github.arenaShooter.Main;
import io.github.arenaShooter.ui.HealthBar;

public class Slime extends Enemy {
    private float damageTimer = 0f;
    private Vector2 attackVelocity;

    private final float REST_DURATION = 0.5f;
    private final float CHARGING_DURATION = 0.5f;
    private final float ATTACK_DURATION = 1f;
    private final float DAMAGE_ON_CHARGE_MULTIPLAYER = 2f;

    private final TextureAtlas textureAtlas = new TextureAtlas(Gdx.files.internal("slime.atlas"));
    private final TextureAtlas deathAnimationAtlas = new TextureAtlas(Gdx.files.internal("death.atlas"));
    protected Animation<TextureRegion> chargeAnimation;
    private final Sound deathSound = Gdx.audio.newSound(Gdx.files.internal("death_sound.mp3"));

    public Slime(float startX, float startY, Main game) {
        this.textureHeight = textureWidth = 48;
        this.hitboxHeight = 27;
        this.hitboxWidth = textureWidth / 2;
        this.hitbox = new Rectangle(startX, startY, hitboxWidth, hitboxHeight);
        this.baseDamage = this.damage = 25f;
        this.baseSpeed = this.speed = 90f;
        this.maxHealth = this.health = 100;
        this.range = 150f;
        this.game = game;

        Array<TextureRegion> walkFrames = new Array<>();
        for (int i = 1; i < 3; i++) {
            walkFrames.add(textureAtlas.findRegion("slime_walk_" + i));
        }
        walkAnimation = new Animation<>(0.15f, walkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> attackFrames = new Array<>();
        attackFrames.add(textureAtlas.findRegion("slime_attack"));
        attackAnimation = new Animation<>(0.15f, attackFrames, Animation.PlayMode.NORMAL);

        Array<TextureRegion> chargeFrames = new Array<>();
        chargeFrames.add(textureAtlas.findRegion("slime_attack"));
        chargeFrames.add(textureAtlas.findRegion("slime_walk_1"));
        chargeAnimation = new Animation<>(0.15f, chargeFrames, Animation.PlayMode.LOOP);

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
                    state = State.CHARGING;

                    stateTime = 0f;
                }
                break;

            case CHARGING:
                stateTime += delta;

                if (stateTime >= CHARGING_DURATION) {
                    stateTime = 0f;
                    state = State.ATTACK;
                    this.attackVelocity = new Vector2(dx + game.player.hitbox.getWidth() / 2 , dy + game.player.hitbox.getWidth() / 2).nor().scl(speed * 3);
                }
                break;

            case ATTACK:
                if (stateTime >= ATTACK_DURATION) {
                    stateTime = 0f;
                    state = State.IDLE;
                } else {
                    hitbox.x += attackVelocity.x * delta;
                    hitbox.y += attackVelocity.y * delta;
                    hitbox.setX(MathUtils.clamp(hitbox.getX(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET + 100f));
                    hitbox.setY(MathUtils.clamp(hitbox.getY(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET + 100f));
                }

                break;

            case IDLE:
                if (stateTime >= REST_DURATION) {
                    stateTime = 0f;
                    state = State.WALK;
                }
                break;

            case DEAD:
                if (deathAnimation.isAnimationFinished(stateTime)) {
                    state = State.ALL_ACTIONS_FINISHED;
                }
                break;
        }

        if (checkPlayerCollision() && damageTimer >= 1f) {
            damageTimer = 0f;

            if (state == State.CHARGING) {
                game.player.takeDamage(DAMAGE_ON_CHARGE_MULTIPLAYER * damage);
            } else if (state != State.DEAD) {
                game.player.takeDamage(damage);
            }
        } else {
            damageTimer += delta;
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
            case CHARGING:
                currentFrame = chargeAnimation.getKeyFrame(stateTime, true);
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
        return textureHeight / 10f;
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
    }


}
