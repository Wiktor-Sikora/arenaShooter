package io.github.arenaShooter.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.arenaShooter.Main;
import io.github.arenaShooter.ui.HealthBar;


public class Zombie extends Enemy {

    private final int DAMAGE_ON_CONTACT = 0;
    private final TextureAtlas textureAtlas = new TextureAtlas(Gdx.files.internal("zombie.atlas"));
    private final TextureAtlas deathAnimationAtlas = new TextureAtlas(Gdx.files.internal("death.atlas"));
    private final Sound deathSound = Gdx.audio.newSound(Gdx.files.internal("death_sound.mp3"));
    private boolean hasDealtDamageThisAttack = false;

    private float attackTimer = 0f;

    public Zombie(float startX, float startY, Main game) {
        this.textureHeight = textureWidth = 67;
        this.hitboxHeight = 27;
        this.hitboxWidth = textureWidth / 2;
        this.hitbox = new Rectangle(startX, startY, hitboxWidth, hitboxHeight);
        this.baseSpeed = 80f;
        this.speed = 80f;
        this.baseDamage = 30f;
        this.damage = 30f;
        this.maxHealth = this.health = 100;
        this.range = 16f;
        this.rateOfFire = 1f;
        this.game = game;

        Array<TextureRegion> walkFrames = new Array<>();
        for (int i = 0; i < 3; i++) {
            walkFrames.add(textureAtlas.findRegion("zombie_walk_" + i));
        }
        walkAnimation = new Animation<>(0.15f, walkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> attackFrames = new Array<>();
        for (int i = 0; i < 2; i++) {
            attackFrames.add(textureAtlas.findRegion("zombie_attack_" + i));
        }
        attackAnimation = new Animation<>(0.5f, attackFrames, Animation.PlayMode.NORMAL);

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
                }
                break;

            case ATTACK:
                stateTime += delta;
                attackTimer += delta;

                int frameIndex = attackAnimation.getKeyFrameIndex(stateTime);

                if (frameIndex == 1 && !hasDealtDamageThisAttack) {
                    if (checkPlayerCollision()) {
                        game.player.takeDamage(this.damage);
                    }
                    hasDealtDamageThisAttack = true;
                }

                if (attackAnimation.isAnimationFinished(stateTime)) {
                    state = State.WALK;
                    stateTime = 0f;
                    attackTimer = 0f;
                    hasDealtDamageThisAttack = false;
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
}
