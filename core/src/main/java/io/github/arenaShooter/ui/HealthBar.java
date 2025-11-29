package io.github.arenaShooter.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import io.github.arenaShooter.Main;

public class HealthBar {
    private ProgressBar.ProgressBarStyle healthBarStyle;
    private ProgressBar healthBar;
    private Main game;

    public HealthBar(Main game, float maxHealth, int entityWidth) {
        this.game = game;

        healthBarStyle = new ProgressBar.ProgressBarStyle();

        Pixmap backgroundPixmap = new Pixmap(entityWidth, 8, Pixmap.Format.RGBA8888);
        backgroundPixmap.setColor(Color.GRAY);
        backgroundPixmap.fill();

        Pixmap knobPixmap = new Pixmap(1, 6, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.GREEN);
        knobPixmap.fill();

        healthBarStyle.background = new TextureRegionDrawable(new TextureRegion(new Texture(backgroundPixmap)));
        healthBarStyle.knobBefore = new TextureRegionDrawable(new TextureRegion(new Texture(knobPixmap)));

        backgroundPixmap.dispose();
        knobPixmap.dispose();

        healthBar = new ProgressBar(0, maxHealth, 1f, false, healthBarStyle);
        healthBar.setSize(entityWidth, 8);
        healthBar.setAnimateDuration(0.1f);

        game.stage.addActor(healthBar);
    }

    public void render(float health, float positionX, float positionY) {
        healthBar.setValue(health);
        healthBar.setPosition(positionX, positionY - 15);
    }

    public void dispose() {
        healthBar.remove();
    }
}
