package io.github.arenaShooter.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.arenaShooter.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardMenu {
    private DatabaseManager db;
    private BitmapFont font;
    private GlyphLayout layout;
    private OrthographicCamera camera;
    private List<String> highScoreList = new ArrayList<>();

    private Texture backgroundTexture;
    private Texture whitePixel;

    public ScoreboardMenu(DatabaseManager db, BitmapFont font, GlyphLayout layout, OrthographicCamera camera) {
        this.db = db;
        this.font = font;
        this.layout = layout;
        this.camera = camera;

        backgroundTexture = new Texture("menu.png");
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void loadHighScores() {
        highScoreList.clear();
        try {
            List<String> scores = db.getHighScores();
            if (scores == null || scores.isEmpty()) {
                highScoreList.add("No scores available");
            } else {
                scores.sort((a, b) -> {
                    try {
                        String[] partsA = a.split(" ");
                        String[] partsB = b.split(" ");
                        int goldA = Integer.parseInt(partsA[1]);
                        int goldB = Integer.parseInt(partsB[1]);
                        return Integer.compare(goldB, goldA);
                    } catch (Exception e) {
                        return 0;
                    }
                });

                for (int i = 0; i < Math.min(10, scores.size()); i++) {
                    highScoreList.add((i + 1) + ". " + scores.get(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            highScoreList.clear();
            highScoreList.add("=== ERROR LOADING SCORES ===");
            highScoreList.add("Database error occurred");
        }
    }

    // Scoreboard
    public void render(SpriteBatch batch) {
        if (highScoreList.isEmpty()) return;

        float centerY = camera.position.y;
        float currentY = centerY + 150;

        float startX = camera.position.x - 230;


        batch.begin();

        batch.draw(backgroundTexture,
            camera.position.x - camera.viewportWidth / 2f,
            camera.position.y - camera.viewportHeight / 2f,
            camera.viewportWidth, camera.viewportHeight);

        batch.setColor(0, 0, 0, 0.5f);
        float rectTop = currentY+20;
        float rectBottom = centerY - 200 - 30;
        float rectHeight = rectTop - rectBottom;

        float rectWidth = 480;
        float rectX = camera.position.x - rectWidth / 2f;
        float rectY = rectBottom;

        batch.draw(whitePixel, rectX, rectY, rectWidth, rectHeight);
        batch.setColor(1, 1, 1, 1f);

        font.setColor(Color.WHITE);

        drawCenteredText(batch, font, layout, camera, "=== HIGH SCORES ===", currentY, 1.3f);
        currentY -= 50;
        font.getData().setScale(1.0f);
        for (String line : highScoreList) {
            layout.setText(font, line);
            font.draw(batch, line, startX, currentY);
            currentY -= 30;
        }

        drawCenteredText(batch, font, layout, camera, "[Q] Return", centerY - 200, 1.2f);

        batch.end();
    }

    // Return by pressing F3
    public boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.Q)) {
            return true;
        }
        return false;
    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, GlyphLayout layout, OrthographicCamera camera,
                                  String text, float y, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        font.draw(batch, text, camera.position.x - layout.width / 2f, y);
    }
}
