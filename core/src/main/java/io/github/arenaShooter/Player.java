package io.github.arenaShooter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import io.github.arenaShooter.ui.HealthBar;
import io.github.arenaShooter.weapons.Gun;
import io.github.arenaShooter.weapons.Weapon;


public class  Player extends Entity {
    public float speed;
    public boolean alive = true;

    public Weapon weapon;

    Animation<TextureRegion> sideWalkAnimation;
    Animation<TextureRegion> frontWalkAnimation;
    Animation<TextureRegion> backWalkAnimation;

    private TextureRegion currentFrame;
    private float stateTime = 0f;
    public boolean facingLeft = false;
    public float rotation = 0f;


    private final TextureAtlas textureAtlas = new TextureAtlas(Gdx.files.internal("player.atlas"));

    public int gold = 0;
    public int dmg = 0;
    public int dmgTaken = 0;
    public int goldEarned = 0;
    public int enemiesKilled = 0;
    public int playerId = 0;

    public Player(float startX, float startY, Main game) {
        this.textureHeight = textureWidth = 64;
        this.hitboxHeight = textureHeight;
        this.hitboxWidth = textureWidth / 2;
        this.hitbox = new Rectangle(startX, startY, hitboxWidth, hitboxHeight);

        this.maxHealth = 100;
        this.health = 100;

        this.speed = 170f;
        this.game = game;

        this.weapon = new Gun(game);
        this.weapon.damage += dmg;

        this.healthBar = new HealthBar(game, maxHealth, (int)textureWidth);

        Array<TextureRegion> sideWalkFrames = new Array<>();
        for (int i = 0; i < 5; i++) {
            sideWalkFrames.add(textureAtlas.findRegion("player_side_" + i));
        }
        sideWalkAnimation = new Animation<>(0.2f, sideWalkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> frontWalkFrames = new Array<>();
        for (int i = 0; i < 3; i++) {
            frontWalkFrames.add(textureAtlas.findRegion("player_front_" + i));
        }
        frontWalkAnimation = new Animation<>(0.3f, frontWalkFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> backWalkFrames = new Array<>();
        for (int i = 0; i < 3; i++) {
            backWalkFrames.add(textureAtlas.findRegion("player_back_" + i));
        }
        backWalkAnimation = new Animation<>(0.3f, backWalkFrames, Animation.PlayMode.LOOP);

        currentFrame = sideWalkAnimation.getKeyFrame(3, false);
    }

    public void equipWeapon(Weapon newWeapon) {
        if(this.weapon != null) {
            this.weapon.dispose();
        }
        this.weapon = newWeapon;
        System.out.println("Selected weapon: " + newWeapon.name);
    }


    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);

        Vector3 mousePos = game.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        Vector2 dir = new Vector2(mousePos.x - getCenterX(), mousePos.y - getCenterY()).nor();

        boolean flipped = false;

        if (Math.abs(dir.x) > Math.abs(dir.y)) {
            if (dir.x < 0) flipped = true;
        }

        rotation = dir.angleDeg();

        if (rotation > 45 && rotation < 135) {
            weapon.render(batch);
        }

        float drawX = hitbox.x + (hitboxWidth - textureWidth) / 2f;
        float drawY = hitbox.y + (hitboxHeight - textureHeight) / 2f;

        if (facingLeft) {
            batch.draw(currentFrame, drawX + textureWidth, drawY, -textureWidth, textureHeight);
        } else {
            batch.draw(currentFrame, drawX, drawY, textureWidth, textureHeight);
        }

        if (!(rotation > 45 && rotation < 135)) {
            weapon.render(batch);
        }
    }

    @Override
    public void update(float delta) {
        stateTime += delta;

        // movement and direction
        handleInput(delta);

        // player does not exceed the border of the map
        hitbox.setX(MathUtils.clamp(hitbox.getX(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET + 100f));
        hitbox.setY(MathUtils.clamp(hitbox.getY(), game.AREA_OFFSET, game.PLAYABLE_AREA_SIZE - game.AREA_OFFSET + 100f));
    }

    public void handleInput(float delta) {
        boolean moving = false;
        boolean localMovementAllowed = !game.isClientNetworkMode();

        if (Gdx.input.isTouched(Input.Buttons.LEFT)) {
            Vector3 unprojectedCords = game.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
            Vector2 direction = new Vector2(unprojectedCords.x - getCenterX(), unprojectedCords.y - getCenterY());
            weapon.setPlayerId(playerId);
            weapon.shoot(direction);
            game.sendNetworkShoot(getCenterX(), getCenterY(), direction.x, direction.y, weapon.name);
        }

        if (localMovementAllowed && Gdx.input.isKeyPressed(Input.Keys.W)) {
            hitbox.setY(hitbox.getY() + speed * delta);
            moving = true;
        }
        if (localMovementAllowed && Gdx.input.isKeyPressed(Input.Keys.S)) {
            hitbox.setY(hitbox.getY() - speed * delta);
            moving = true;
        }
        if (localMovementAllowed && Gdx.input.isKeyPressed(Input.Keys.A)) {
            hitbox.setX(hitbox.getX() - speed * delta);
            moving = true;
        }
        if (localMovementAllowed && Gdx.input.isKeyPressed(Input.Keys.D)) {
            hitbox.setX(hitbox.getX() + speed * delta);
            moving = true;
        }

        Vector3 mousePos = game.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f));
        Vector2 direction = new Vector2(mousePos.x - getCenterX(), mousePos.y - getCenterY()).nor();

        float angle = direction.angleDeg();

        if (angle >= 45 && angle < 135) {
            currentFrame = backWalkAnimation.getKeyFrame(stateTime, true);
            facingLeft = false;
        } else if (angle >= 135 && angle < 225) {
            currentFrame = sideWalkAnimation.getKeyFrame(stateTime, true);
            facingLeft = true;
        } else if (angle >= 225 && angle < 315) {
            currentFrame = frontWalkAnimation.getKeyFrame(stateTime, true);
            facingLeft = false;
        } else {
            currentFrame = sideWalkAnimation.getKeyFrame(stateTime, true);
            facingLeft = false;
        }

        if (moving) {
            stateTime += delta;
        } else {
            stateTime = 4;
        }
    }

    @Override
    public void takeDamage(float amount) {
        if (health <= 0) return;

        health -= amount;

        this.dmgTaken += ((int) amount);

        if (health <= 0) {
            health = 0;
            kill();
        }
    }

    public void dispose() {
        super.dispose();

        if (textureAtlas != null) {
            textureAtlas.dispose();
        }
    }

    @Override
    public void kill() {
        alive = false;
        System.out.println("=== GAME OVER ===");
        System.out.println("Wave: " + game.waveNumber);
        System.out.println("Collected gold: " + gold);
    }
}
