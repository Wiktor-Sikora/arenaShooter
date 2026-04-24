package io.github.arenaShooter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GameLogger {
    private static final Object LOCK = new Object();
    private static final Path LOG_PATH = Path.of("log.txt");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private GameLogger() {
    }

    public static void log(String infoType, String description) {
        if (infoType == null || infoType.isBlank() || description == null || description.isBlank()) {
            return;
        }

        String line = DATE_FORMAT.format(LocalDateTime.now()) + " " + infoType + " " + description + System.lineSeparator();

        synchronized (LOCK) {
            try {
                Files.writeString(
                    LOG_PATH,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            } catch (IOException ignored) {
                // Logging must not interrupt gameplay.
            }
        }
    }
}
