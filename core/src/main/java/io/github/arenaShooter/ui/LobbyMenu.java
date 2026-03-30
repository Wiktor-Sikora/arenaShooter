package io.github.arenaShooter.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.arenaShooter.Main;

import java.util.List;

public class LobbyMenu {
    public String lobbyAddress;
    private Main main;

    public static class LobbyPlayer {
        public final int playerId;
        public final boolean ready;
        public final String clientId;

        public LobbyPlayer(int playerId, boolean ready, String clientId) {
            this.playerId = playerId;
            this.ready = ready;
            this.clientId = clientId;
        }
    }

    public LobbyMenu(Main main, String lobbyAddress) {
        this.lobbyAddress = lobbyAddress;
        this.main = main;
    }

    public void render(SpriteBatch batch, BitmapFont font, GlyphLayout layout, OrthographicCamera camera,
                       boolean host, boolean localReady, List<LobbyPlayer> players, String status) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float centerY = camera.position.y;
        font.setColor(Color.WHITE);
        drawCenteredText(batch, font, layout, camera, (host ? "Hosting Lobby on" : "Client in Lobby") + " " + lobbyAddress, centerY + 145, 2.2f);

        font.setColor(Color.LIGHT_GRAY);
        drawCenteredText(batch, font, layout, camera,
            String.format("[R] Toggle Ready, [Q] Quit%s", host ? ", [Enter] Start when everyone is ready" : ""),
            centerY + 105, 1.0f);
        drawCenteredText(batch, font, layout, camera,
            "Your status: " + (localReady ? "READY" : "NOT READY"), centerY + 78, 1.0f);

        float y = centerY + 40;
        for (LobbyPlayer player : players) {
            String shortId = player.clientId;
            if (shortId.length() > 8) {
                shortId = shortId.substring(0, 8);
            }
            drawCenteredText(batch, font, layout, camera,
                "Player " + player.playerId + " (" + shortId + ") - " + (player.ready ? "READY" : "NOT READY"), y, 1.0f);
            y -= 24f;
        }

        drawCenteredText(batch, font, layout, camera, status, centerY - 120, 1.0f);
        batch.end();
    }

    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            main.gameState = Main.GameState.MENU;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            main.lobbyLocalReady = !main.lobbyLocalReady;
            main.sendReadyState();
            return;
        }

        if (main.menu.startMode == Menu.NetworkMode.HOST && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (!main.areAllLobbyPlayersReady()) {
                main.lobbyStatus = "All connected clients must be ready.";
                return;
            }
            main.requestLobbyStart();
        }

    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, GlyphLayout layout, OrthographicCamera camera,
                                  String text, float y, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        font.draw(batch, text, camera.position.x - layout.width / 2f, y);
    }
}
