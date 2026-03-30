package io.github.arenaShooter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:../score.db";


    private Connection connection;

    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("Baza danych zainicjalizowana pomyślnie");
        } catch (ClassNotFoundException e) {
            System.err.println("Nie znaleziono sterownika SQLite: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Błąd inicjalizacji bazy danych: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void createTables() {
        try {
            String dbPath = DB_URL.replace("jdbc:sqlite:", "");
            java.io.File dbFile = new java.io.File(dbPath);
            System.out.println("Plik bazy danych istnieje: " + dbFile.exists());
            System.out.println("Ścieżka absolutna: " + dbFile.getAbsolutePath());

            String scoreTable = "CREATE TABLE IF NOT EXISTS high_scores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "gold_earned INTEGER NOT NULL, " +
                "enemies_killed INTEGER NOT NULL, " +
                "damage_taken INTEGER NOT NULL, " +
                "date DATETIME DEFAULT CURRENT_TIMESTAMP);";

            try (Statement stmt = connection.createStatement()) {
                System.out.println("Wykonywanie zapytania: " + scoreTable);
                stmt.execute(scoreTable);
                System.out.println("Tabela utworzona pomyślnie");
            } catch (SQLException e) {
                System.err.println("Błąd tworzenia tabeli: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Błąd w createTables: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void saveScore(int goldEarned, int enemiesKilled, int damageTaken) {
        String sql = "INSERT INTO high_scores(gold_earned, enemies_killed, damage_taken) VALUES(?,?,?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, goldEarned);
            pstmt.setInt(2, enemiesKilled);
            pstmt.setInt(3, damageTaken);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> loadScores() {
        List<String> scores = new ArrayList<>();
        String sql = "SELECT gold_earned, enemies_killed, damage_taken, date FROM high_scores ORDER BY gold_earned DESC LIMIT 10";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int gold = rs.getInt("gold_earned");
                int kills = rs.getInt("enemies_killed");
                int damage = rs.getInt("damage_taken");
                String date = rs.getString("date");

                scores.add(String.format("Gold: %d, Kills: %d, Damage: %d (%s)", gold, kills, damage, date));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return scores;
    }

    public List<String> getHighScores() {
        List<String> scores = new ArrayList<>();

        if (connection == null) {
            System.err.println("Brak połączenia z bazą.");
            return scores;
        }

        String sql = "SELECT gold_earned, enemies_killed, damage_taken, date FROM high_scores ORDER BY gold_earned DESC LIMIT 10";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int gold = rs.getInt("gold_earned");
                int kills = rs.getInt("enemies_killed");
                int damage = rs.getInt("damage_taken");
                String date = rs.getString("date");

                scores.add(String.format("Gold: %d, Kills: %d, Damage: %d (%s)", gold, kills, damage, date));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return scores;
    }



    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

