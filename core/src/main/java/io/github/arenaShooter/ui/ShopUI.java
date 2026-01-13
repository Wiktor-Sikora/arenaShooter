package io.github.arenaShooter.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.arenaShooter.Main;
import io.github.arenaShooter.Player;
import io.github.arenaShooter.weapons.Gun;
import io.github.arenaShooter.weapons.Shotgun;
import io.github.arenaShooter.weapons.Uzi;

import java.lang.String;
import java.util.*;

public class ShopUI {
    private Main game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GlyphLayout layout;

    private List<ShopItem> allItems = new ArrayList<>();
    private List<ShopItem> currentItems = new ArrayList<>();

    // Tekstury
    private Texture gunTexture, shotgunTexture, uziTexture, potionTexture, coinTexture, bootTexture;


    // CENY
    public static final int GUN_PRICE = 0;
    public static final int SHOTGUN_PRICE = 200;
    public static final int UZI_PRICE = 200;
    public static final int POTION_PRICE = 100;
    public static final int POTION_HEAL = 50;
    public static final int BOOT_PRICE = 300;
    public static final int BOOT_BOOST = 5;

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
        gunTexture = loadTexture("gun.png");
        shotgunTexture = loadTexture("shotgun.png");
        uziTexture = loadTexture("uzi_icon.png");
        potionTexture = loadTexture("potion.png");
        coinTexture = loadTexture("coin.png");
        bootTexture = loadTexture("winged_boot.png");

        allItems.add(new ShopItem("MIKSTURA", "+50 HP", POTION_PRICE, potionTexture, false));
        allItems.add(new ShopItem("PISTOLET", "Podstawowy", GUN_PRICE, gunTexture, false));
        allItems.add(new ShopItem("SHOTGUN", "Rozrzut", SHOTGUN_PRICE, shotgunTexture, false));
        allItems.add(new ShopItem("UZI", "Szybki ogien", UZI_PRICE, uziTexture, false));
        allItems.add(new ShopItem("WINGED BOOTS", "+5 Speed", BOOT_PRICE, bootTexture, true));
    }

    private static class ShopItem {
        String name;
        String desc;
        int price;
        Texture texture;

        boolean oncePerWave;
        boolean boughtThisWave;

        ShopItem(String name, String desc, int price, Texture texture, boolean oncePerWave) {
            this.name = name;
            this.desc = desc;
            this.price = price;
            this.texture = texture;

            this.oncePerWave = oncePerWave;
            this.boughtThisWave = false;
        }
    }

    public void buyItem(int index) {
        if (index < 0 || index >= currentItems.size()) return;

        ShopItem item = currentItems.get(index);

        Player player = game.player;

        if (player.gold < item.price) {
            System.out.println("Za malo zlota!");
            return;
        }

        if (item.oncePerWave && item.boughtThisWave) {
            System.out.println("Ten przedmiot mozna kupic tylko raz na fale!");
            return;
        }

        switch (item.name) {

            case "PISTOLET":
                player.equipWeapon(new Gun(game));
                break;

            case "SHOTGUN":
                player.gold -= item.price;
                player.equipWeapon(new Shotgun(game));
                break;

            case "UZI":
                player.gold -= item.price;
                player.equipWeapon(new Uzi(game));
                break;

            case "MIKSTURA":
                if (player.health >= player.maxHealth) {
                    System.out.println("Masz pelne HP!");
                    return;
                }
                player.gold -= item.price;

                if (player.health+POTION_HEAL <= player.maxHealth){
                    player.health += POTION_HEAL;
                } else {
                    player.health = player.maxHealth;
                }

                break;

            case "WINGED BOOTS":
                    player.gold -= item.price;
                    game.player.speed += BOOT_BOOST;
                break;
        }

        if (item.oncePerWave) {
            item.boughtThisWave = true;
        }

        System.out.println("Kupiono: " + item.name);
    }


    private Texture loadTexture(String path) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (Exception e) {
            return null;
        }
    }

    public void randomizeShop() {
        currentItems.clear();

        List<ShopItem> temp = new ArrayList<>(allItems);
        temp.remove(0);
        currentItems.add(allItems.get(0));
        Collections.shuffle(temp);

        // max 4 items
        for (int i = 0; i < Math.min(4, temp.size()); i++) {
            currentItems.add(temp.get(i));
        }
    }

    public void resetWavePurchases() {
        for (ShopItem item : currentItems) {
            item.boughtThisWave = false;
        }
    }

    public void render() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight()+80;
        float centerX = screenW / 2;
        float centerY = screenH / 2;

        // --- 1. Przyciemnienie tła ---
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.65f);
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
        int index = 0;

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                if (index >= currentItems.size()) break;

                ShopItem item = currentItems.get(index);

                float x = startX + col * (tileW + gap);
                float y = startY - tileH - row * (tileH + gap);

                drawTile(
                    index + 1,
                    item,
                    x, y, tileW, tileH
                );

                index++;
            }
        }

        // --- 4. Instrukcja na dole ---
        batch.begin();
        font.getData().setScale(1.3f);
        font.setColor(Color.GREEN);
        drawCenteredText("Nacisnij [ENTER] aby walczyc dalej!", 50);
        batch.end();
    }

    private void drawTile(int key, ShopItem item, float x, float y, float w, float h) {
        boolean isEquipped = false;
        // Sprawdź czy to aktualna broń
        if (item.name.equalsIgnoreCase("PISTOLET") && game.player.weapon.name.contains("Pistolet")) isEquipped = true;
        if (item.name.equalsIgnoreCase("UZI") && game.player.weapon.name.contains("Uzi")) isEquipped = true;
        if (item.name.equalsIgnoreCase("SHOTGUN") && game.player.weapon.name.contains("Shotgun")) isEquipped = true;

        // Potion nigdy nie jest "equipped", ale sprawdzamy czy HP jest pełne
        boolean isFullHp = item.name.equalsIgnoreCase("MIKSTURA") && game.player.health >= game.player.maxHealth;

        boolean canAfford = game.player.gold >= item.price;

        // --- KOLORYSTYKA ---
        Color bgColor;
        Color borderColor;


        if (isEquipped) {
            bgColor = new Color(0.1f, 0.3f, 0.1f, 0.9f); // Ciemna zieleń
            borderColor = Color.LIME;
        } else if (isFullHp || item.oncePerWave && item.boughtThisWave) {
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
        float maxIconSize = 80f;

        float yOffset = 0;

        if (item.name.equalsIgnoreCase("SHOTGUN")) {
            yOffset = -15;
        }

        float texW = item.texture.getWidth();
        float texH = item.texture.getHeight();

        float scale = maxIconSize / Math.max(texW, texH);

        float drawW = texW * scale;
        float drawH = texH * scale;

        batch.draw(item.texture,
            x + (w - drawW) / 2,
            y + (h - drawH) - 30 + yOffset,
            drawW,
            drawH
        );

        // Nazwa
        font.getData().setScale(1.8f);
        font.setColor(borderColor);
        layout.setText(font, item.name);
        font.draw(batch, item.name, x + (w - layout.width) / 2, y + h - 130);

        // Opis
        font.getData().setScale(1.1f);
        font.setColor(Color.LIGHT_GRAY);
        layout.setText(font, item.desc);
        font.draw(batch, item.desc, x + (w - layout.width) / 2, y + h - 110);

        // Cena (na samym dole)
        font.getData().setScale(1.5f);
        String priceText;
        if (isEquipped) {
            font.setColor(Color.GREEN);
            priceText = "WYPOSAZONO";
        } else if (isFullHp) {
            font.setColor(Color.GRAY);
            priceText = "PELNE ZDROWIE";
        } else if (item.oncePerWave && item.boughtThisWave) {
            priceText = "KUPIONE";
        } else if (item.price == 0) {
            font.setColor(Color.GREEN);
            priceText = "DARMOWE";
        } else {
            font.setColor(canAfford ? Color.GOLD : Color.RED);
            priceText = item.price + "";
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
