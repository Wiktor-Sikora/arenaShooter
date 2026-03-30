package io.github.arenaShooter.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Menu {
    public enum StartMode {
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

    public Menu(String initialClientHost, String hostingHost, int initialPort) {
        this.hostingHost = hostingHost;
        this.clientHost = initialClientHost;
        this.hostInput = initialClientHost;
        this.port = initialPort;
        this.portInput = String.valueOf(initialPort);
        this.status = "TAB switch field, type value. F1 Host, F2 Join";
    }

    public StartMode pollStartMode() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            editField = editField == EditField.HOST ? EditField.PORT : EditField.HOST;
            return StartMode.NONE;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            removeLastCharacter();
            return StartMode.NONE;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            if (!applyPortSettings()) {
                return StartMode.NONE;
            }
            status = "Starting host on " + hostingHost + ":" + port + "...";
            return StartMode.HOST;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            if (!applyClientSettings()) {
                return StartMode.NONE;
            }
            status = "Connecting to " + clientHost + ":" + port + "...";
            return StartMode.CLIENT;
        }

        return StartMode.NONE;
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

        float centerY = camera.position.y;
        font.setColor(Color.WHITE);
        drawCenteredText(batch, font, layout, camera, "Arena Shooter", centerY + 140, 2.5f);

        font.setColor(Color.LIGHT_GRAY);
        drawCenteredText(batch, font, layout, camera, "Host IP (fixed for server): " + hostingHost, centerY + 85, 1.0f);
        drawCenteredText(batch, font, layout, camera,
            (editField == EditField.HOST ? "> " : "") + "Client target IP: " + hostInput, centerY + 55, 1.1f);
        drawCenteredText(batch, font, layout, camera,
            (editField == EditField.PORT ? "> " : "") + "Port: " + portInput, centerY + 30, 1.1f);
        drawCenteredText(batch, font, layout, camera, "[F1] Create Server", centerY + 5, 1.3f);
        drawCenteredText(batch, font, layout, camera, "[F2] Connect To Server", centerY - 25, 1.3f);
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
