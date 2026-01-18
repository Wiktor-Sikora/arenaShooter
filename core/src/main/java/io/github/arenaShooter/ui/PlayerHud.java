package io.github.arenaShooter.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.arenaShooter.Main;

import java.util.Objects;

/**
 * ============================================
 * ZAKTUALIZOWANA KLASA - HUD gracza podczas gry
 * ============================================
 * Wyświetla: złoto, HP, broń, numer fali
 */
public class PlayerHud {
    protected Main game;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture coinTexture;

    private boolean showExtraStats = false;

    private static final Color GOLD_COLOR = new Color(1f, 0.84f, 0f, 1f);
    private static final Color HP_GREEN = new Color(0.2f, 0.8f, 0.2f, 1f);
    private static final Color HP_RED = new Color(0.9f, 0.2f, 0.2f, 1f);

    public PlayerHud(Main game) {
        this.game = game;

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        font = new BitmapFont();
        font.getData().setScale(1.3f);

        // Tekstura monety
        Pixmap pixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        pixmap.setColor(GOLD_COLOR);
        pixmap.fillCircle(10, 10, 9);
        coinTexture = new Texture(pixmap);
        pixmap.dispose();

        coinTexture = new Texture(Gdx.files.internal("coin.png"));
    }

    public void render() {
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();
        float hudY = H + 35;

        batch.begin();

        // Złoto
        batch.draw(coinTexture, 10, hudY - 5, 22, 22);
        font.setColor(GOLD_COLOR);
        font.draw(batch, "" + game.player.gold, 38, hudY + 12);

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.TAB)) {
            showExtraStats = !showExtraStats;
        }

        if (showExtraStats) {
            float statsX = 10;
            float statsY = hudY - 25; // pod HP
            float lineGap = -25;

            font.setColor(Color.LIGHT_GRAY);

            font.draw(batch, "Speed: " + game.player.speed, statsX, statsY);
            if (Objects.equals(game.player.weapon.name, "Shotgun")) {
                font.draw(batch, "DMG: " + game.player.weapon.damage + " x 6", statsX, statsY + lineGap);
            } else {
                font.draw(batch, "DMG: " + game.player.weapon.damage, statsX, statsY + lineGap);
            }


        }

        batch.end();

        // Pasek HP
        float hpX = 100;
        float hpW = 120;
        float hpH = 18;
        float hpY = hudY - 2;
        float hpPercent = game.player.health / game.player.maxHealth;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(hpX, hpY, hpW, hpH);
        shapeRenderer.setColor(hpPercent > 0.3f ? HP_GREEN : HP_RED);
        shapeRenderer.rect(hpX, hpY, hpW * hpPercent, hpH);
        shapeRenderer.end();

        batch.begin();

        // HP tekst
        font.setColor(Color.WHITE);
        font.draw(batch, (int)game.player.health + "/" + (int)game.player.maxHealth, hpX + hpW + 8, hudY + 12);

        // Broń
        font.setColor(Color.CYAN);
        font.draw(batch, game.player.weapon.name, 320, hudY + 12);

        // Fala
        font.setColor(Color.YELLOW);
        font.draw(batch, "Fala: " + game.waveNumber, W - 100, hudY + 12);

        batch.end();
    }

    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (coinTexture != null) coinTexture.dispose();
    }
}
