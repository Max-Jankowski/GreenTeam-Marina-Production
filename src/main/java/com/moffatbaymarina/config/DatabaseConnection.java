package com.moffatbaymarina.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central database connection utility for Moffat Bay Marina.
 *
 * Reads connection settings from src/main/resources/db.properties.
 */
public final class DatabaseConnection {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "db.properties was not found in src/main/resources.");
            }

            PROPERTIES.load(input);

            String driver = PROPERTIES.getProperty(
                    "db.driver",
                    "com.mysql.cj.jdbc.Driver");

            Class.forName(driver);

        } catch (IOException | ClassNotFoundException e) {
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
                    "Missing required database property: " + key);
        }

        return value.trim();
    }
}
