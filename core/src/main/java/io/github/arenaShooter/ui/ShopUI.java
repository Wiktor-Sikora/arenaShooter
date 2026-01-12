package io.github.arenaShooter.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import io.github.arenaShooter.Main;

public class ShopUI {
    private Main game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GlyphLayout layout;

    // Tekstury
    private Texture gunTexture;
    private Texture shotgunTexture;
    private Texture uziTexture;
    private Texture potionTexture;
    private Texture coinTexture;

    // CENY
    public static final int GUN_PRICE = 0;
    public static final int SHOTGUN_PRICE = 200;
    public static final int UZI_PRICE = 200;
    public static final int POTION_PRICE = 100;
    public static final int POTION_HEAL = 50;

    // Statystyki
    private int enemiesKilled = 0;
    private int goldEarned = 0;
    private int damageTaken = 0;

    public ShopUI(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font = new BitmapFont();
        this.layout = new GlyphLayout();

        // Ładowanie tekstur
        gunTexture = loadTexture("gun_icon.png");
        shotgunTexture = loadTexture("shotgun_icon.png");
        uziTexture = loadTexture("uzi_icon.png");
        potionTexture = loadTexture("potion.png");
        coinTexture = loadTexture("coin.png");
    }

    private Texture loadTexture(String path) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (Exception e) {
            return null;
        }
    }

    public void render() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float centerX = screenW / 2;
        float centerY = screenH / 2;

        // --- 1. Przyciemnienie tła ---
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.85f);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.end();

        batch.begin();

        // --- 2. Nagłówek ---
        font.getData().setScale(3f);
        font.setColor(Color.ORANGE);
        drawCenteredText("SKLEP - FALA " + game.waveNumber, screenH - 50);

        // Statystyki gracza
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);
        drawCenteredText("Posiadasz: " + game.player.gold + " zl  |  HP: " + (int)game.player.health + "/" + (int)game.player.maxHealth, screenH - 100);

        batch.end();

        // --- 3. Siatka Kafelków (2x2) ---
        // Konfiguracja wymiarów
        float tileW = 350;
        float tileH = 200;
        float gap = 30; // Odstęp między kafelkami

        // Obliczamy pozycje startowe tak, by całość była idealnie na środku
        float totalGridW = (tileW * 2) + gap;
        float totalGridH = (tileH * 2) + gap;
        float startX = centerX - (totalGridW / 2);
        float startY = centerY + (totalGridH / 2) - 60; // -60 żeby zrobić miejsce na nagłówek

        // Rysowanie kafelków
        // Wiersz 1
        drawTile(1, "PISTOLET", "Podstawowy", GUN_PRICE, gunTexture, startX, startY - tileH, tileW, tileH);
        drawTile(2, "SHOTGUN", "Rozrzut", SHOTGUN_PRICE, shotgunTexture, startX + tileW + gap, startY - tileH, tileW, tileH);

        // Wiersz 2
        drawTile(3, "UZI", "Szybki ogien", UZI_PRICE, uziTexture, startX, startY - tileH - tileH - gap, tileW, tileH);
        drawTile(4, "MIKSTURA", "+50 HP", POTION_PRICE, potionTexture, startX + tileW + gap, startY - tileH - tileH - gap, tileW, tileH);

        // --- 4. Instrukcja na dole ---
        batch.begin();
        font.getData().setScale(1.3f);
        font.setColor(Color.GREEN);
        drawCenteredText("Nacisnij [ENTER] aby walczyc dalej!", 50);
        batch.end();
    }

    private void drawTile(int key, String name, String desc, int price, Texture texture, float x, float y, float w, float h) {
        boolean isEquipped = false;
        // Sprawdź czy to aktualna broń
        if (name.equalsIgnoreCase("PISTOLET") && game.player.weapon.name.contains("Pistolet")) isEquipped = true;
        if (name.equalsIgnoreCase("UZI") && game.player.weapon.name.contains("Uzi")) isEquipped = true;
        if (name.equalsIgnoreCase("SHOTGUN") && game.player.weapon.name.contains("Shotgun")) isEquipped = true;

        // Potion nigdy nie jest "equipped", ale sprawdzamy czy HP jest pełne
        boolean isFullHp = name.equalsIgnoreCase("MIKSTURA") && game.player.health >= game.player.maxHealth;

        boolean canAfford = game.player.gold >= price;

        // --- KOLORYSTYKA ---
        Color bgColor;
        Color borderColor;

        if (isEquipped) {
            bgColor = new Color(0.1f, 0.3f, 0.1f, 0.9f); // Ciemna zieleń
            borderColor = Color.LIME;
        } else if (isFullHp) {
            bgColor = new Color(0.2f, 0.2f, 0.2f, 0.9f); // Szary
            borderColor = Color.GRAY;
        } else if (canAfford) {
            bgColor = new Color(0.2f, 0.2f, 0.3f, 0.9f); // Granatowy
            borderColor = Color.CYAN; // Lub GOLD
        } else {
            bgColor = new Color(0.3f, 0.1f, 0.1f, 0.9f); // Czerwony
            borderColor = Color.RED;
        }

        // --- TŁO KAFELKA ---
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.end();

        // --- RAMKA KAFELKA ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(borderColor);
        // Rysujemy grubszą ramkę (kilka prostokątów)
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.rect(x + 1, y + 1, w - 2, h - 2);
        shapeRenderer.end();

        // --- ZAWARTOŚĆ (Tekst i Ikony) ---
        batch.begin();

        // Numer klawisza (lewy górny róg)
        font.getData().setScale(1.5f);
        font.setColor(Color.YELLOW);
        font.draw(batch, "[" + key + "]", x + 10, y + h - 10);

        // Ikona (Na środku, przesunięta w górę)
        float iconSize = 80;
        if (texture != null) {
            batch.draw(texture, x + (w - iconSize) / 2, y + h - iconSize - 30, iconSize, iconSize);
        }

        // Nazwa
        font.getData().setScale(1.8f);
        font.setColor(borderColor);
        layout.setText(font, name);
        font.draw(batch, name, x + (w - layout.width) / 2, y + h - 120);

        // Opis
        font.getData().setScale(1.1f);
        font.setColor(Color.LIGHT_GRAY);
        layout.setText(font, desc);
        font.draw(batch, desc, x + (w - layout.width) / 2, y + h - 150);

        // Cena (na samym dole)
        font.getData().setScale(1.5f);
        String priceText;
        if (isEquipped) {
            font.setColor(Color.GREEN);
            priceText = "WYPOSAZONO";
        } else if (isFullHp) {
            font.setColor(Color.GRAY);
            priceText = "PELNE ZDROWIE";
        } else if (price == 0) {
            font.setColor(Color.GREEN);
            priceText = "DARMOWE";
        } else {
            font.setColor(canAfford ? Color.GOLD : Color.RED);
            priceText = price + "";
        }

        layout.setText(font, priceText);
        font.draw(batch, priceText, x + (w - layout.width) / 2, y + 40);

        batch.end();
    }

    private void drawCenteredText(String text, float y) {
        layout.setText(font, text);
        font.draw(batch, text, (Gdx.graphics.getWidth() - layout.width) / 2, y);
    }

    // Metody pomocnicze
    public void recordKill() { enemiesKilled++; }
    public void recordGold(int amount) { goldEarned += amount; }
    public void recordDamage(int amount) { damageTaken += amount; }
    public void resetStats() {
        enemiesKilled = 0;
        goldEarned = 0;
        damageTaken = 0;
    }

    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (gunTexture != null) gunTexture.dispose();
        if (shotgunTexture != null) shotgunTexture.dispose();
        if (uziTexture != null) uziTexture.dispose();
        if (potionTexture != null) potionTexture.dispose();
        if (coinTexture != null) coinTexture.dispose();
    }
}
