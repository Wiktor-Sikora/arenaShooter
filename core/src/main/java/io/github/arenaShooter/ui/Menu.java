package io.github.arenaShooter.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.arenaShooter.Main;

public class Menu {
    public enum NetworkMode {
        NONE,
        HOST,
        CLIENT
    }

    private enum EditField {
        HOST,
        PORT
    }

    private String hostingHost;
    private String clientHost;
    private String hostInput;
    private String portInput;
    private int port;
    private String status;
    private EditField editField = EditField.HOST;
    private Texture backgroundTexture;
    private Texture whitePixel;
    public Main main;
    public NetworkMode startMode = null;

    public Menu(Main main, String initialClientHost, String hostingHost, int initialPort) {
        this.main = main;
        this.hostingHost = hostingHost;
        this.clientHost = initialClientHost;
        this.hostInput = initialClientHost;
        this.port = initialPort;
        this.portInput = String.valueOf(initialPort);
        this.status = "TAB switch field, type value. F1 Host, F2 Join, F3 Scoreboard";
        backgroundTexture = new Texture("menu.png");

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            editField = editField == EditField.HOST ? EditField.PORT : EditField.HOST;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            main.scoreboardMenu.loadHighScores(); // top 10 scores
            main.gameState = Main.GameState.SCOREBOARD;
            return;
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            removeLastCharacter();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            if (!applyPortSettings()) {
                startMode = NetworkMode.NONE;
                return;
            }
            status = "Starting host on " + hostingHost + ":" + port + "...";
            startMode = NetworkMode.HOST;

            main.startFromMenu(this.startMode);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            if (!applyClientSettings()) {
                startMode = NetworkMode.NONE;
                return;
            }

            status = "Connecting to " + clientHost + ":" + port + "...";
            startMode = NetworkMode.CLIENT;
            main.startFromMenu(this.startMode);
        }
    }

    public boolean handleCharacter(char character) {
        if (character == '\r' || character == '\n' || Character.isISOControl(character)) {
            return false;
        }

        if (editField == EditField.HOST) {
            if (Character.isLetterOrDigit(character) || character == '.' || character == '-' || character == '_') {
                hostInput += character;
                return true;
            }
            return false;
        }

        if (Character.isDigit(character) && portInput.length() < 5) {
            portInput += character;
            return true;
        }

        return false;
    }

    public void render(SpriteBatch batch, BitmapFont font, GlyphLayout layout, OrthographicCamera camera) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        batch.draw(backgroundTexture,
            camera.position.x - camera.viewportWidth / 2f,
            camera.position.y - camera.viewportHeight / 2f,
            camera.viewportWidth, camera.viewportHeight);

        float centerY = camera.position.y;

        batch.setColor(0, 0, 0, 0.5f);
        float rectTop = centerY + 85 + 15;
        float rectBottom = centerY - 100 - 30;
        float rectHeight = rectTop - rectBottom;

        float rectWidth = 430;
        float rectX = camera.position.x - rectWidth / 2f;
        float rectY = rectBottom;

        batch.draw(whitePixel, rectX, rectY, rectWidth, rectHeight);
        batch.setColor(1, 1, 1, 1f);


        font.setColor(Color.WHITE);
        drawCenteredText(batch, font, layout, camera, "Host IP (fixed for server): " + hostingHost, centerY + 85, 1.0f);
        drawCenteredText(batch, font, layout, camera,
            (editField == EditField.HOST ? "> " : "") + "Client target IP: " + hostInput, centerY + 55, 1.1f);
        drawCenteredText(batch, font, layout, camera,
            (editField == EditField.PORT ? "> " : "") + "Port: " + portInput, centerY + 30, 1.1f);
        drawCenteredText(batch, font, layout, camera, "[F1] Create Server", centerY + 5, 1.3f);
        drawCenteredText(batch, font, layout, camera, "[F2] Connect To Server", centerY - 25, 1.3f);
        drawCenteredText(batch, font, layout, camera, "[F3] Scoreboard", centerY - 55, 1.3f);
        drawCenteredText(batch, font, layout, camera, status, centerY - 100, 1.0f);

        batch.end();
    }

    public String getClientHost() {
        return clientHost;
    }

    public String getHostingHost() {
        return hostingHost;
    }

    public int getPort() {
        return port;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private boolean applyClientSettings() {
        if (!applyPortSettings()) {
            return false;
        }

        String host = hostInput == null ? "" : hostInput.trim();
        if (host.isEmpty()) {
            status = "Client host cannot be empty.";
            return false;
        }

        clientHost = host;
        return true;
    }

    private boolean applyPortSettings() {
        int parsedPort = parsePort(portInput, -1);
        if (parsedPort == -1) {
            status = "Port must be 1-65535.";
            return false;
        }

        port = parsedPort;
        return true;
    }

    private void removeLastCharacter() {
        if (editField == EditField.HOST) {
            if (!hostInput.isEmpty()) {
                hostInput = hostInput.substring(0, hostInput.length() - 1);
            }
            return;
        }

        if (!portInput.isEmpty()) {
            portInput = portInput.substring(0, portInput.length() - 1);
        }
    }

    private int parsePort(String rawPort, int defaultPort) {
        if (rawPort == null || rawPort.isBlank()) {
            return defaultPort;
        }

        try {
            int parsed = Integer.parseInt(rawPort);
            if (parsed > 0 && parsed <= 65535) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }

        return defaultPort;
    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, GlyphLayout layout, OrthographicCamera camera,
                                  String text, float y, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        font.draw(batch, text, camera.position.x - layout.width / 2f, y);
    }
}
