package com.moffatbaymarina.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central database connection utility for Moffat Bay Marina.
 *
 * Connection settings come from environment variables (MARINA_DB_URL,
 * MARINA_DB_USER, MARINA_DB_PASSWORD) or an optional local, uncommitted
 * db.properties. See DbSettings for the full list. No password is kept in
 * the repository.
 */
public final class DatabaseConnection {

    private static final Properties PROPERTIES = DbSettings.load();

    static {
        try {
            String driver = PROPERTIES.getProperty(
                    "db.driver",
                    "com.mysql.cj.jdbc.Driver");

            Class.forName(driver);

        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() {
        // Utility class; do not instantiate.
    }

    public static Connection getConnection() throws SQLException {
        String url = requiredProperty("db.url");
        String username = requiredProperty("db.username");
        String password = PROPERTIES.getProperty("db.password", "");

        return DriverManager.getConnection(url, username, password);
    }

    private static String requiredProperty(String key) {
        String value = PROPERTIES.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required database setting: " + key
                    + ". Set the MARINA_DB_URL, MARINA_DB_USER and MARINA_DB_PASSWORD"
                    + " environment variables (see db.properties.example).");
        }

        return value.trim();
    }
}
