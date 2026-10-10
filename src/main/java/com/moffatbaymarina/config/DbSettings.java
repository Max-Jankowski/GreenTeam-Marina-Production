/*
Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 4 CSD-460 

Post the gauntlet of the other teams judgement 
*/


package com.moffatbaymarina.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

 
 // loads DB settings connection 'without' keeping passwords in the repo 
 //The property names stay the same as before (db.url, db.username / db.user, db.password, * db.host, db.port, db.name, db.driver), so existing code keeps working.
public final class DbSettings {

    static final String CONFIG_FILE_VARIABLE = "MARINA_DB_CONFIG";

    private static final String[] KEYS = {
            "db.url", "db.driver", "db.host", "db.port", "db.name",
            "db.username", "db.user", "db.password"
    };

    private DbSettings() {
        // Utility class do not instantiate.
    }

    // Settings for the running application, using the real environment. 
    public static Properties load() {
        return load(System::getenv);
    }

    // The environment is passed in so the lookup rules can be tested without
    // touching real environment variables.
    static Properties load(Function<String, String> environment) {
        Properties settings = new Properties();

        // optional local file on the classpath (developer machines only, a bit overkill as of now I know )
        try (InputStream input = DbSettings.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input != null) {
                settings.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties.", e);
        }

        //  file outside the project
        String externalFile = environment.apply(CONFIG_FILE_VARIABLE);
        if (externalFile != null && !externalFile.isBlank()) {
            try (InputStream input = Files.newInputStream(Path.of(externalFile.trim()))) {
                settings.load(input);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Could not read the file named by " + CONFIG_FILE_VARIABLE + ": "
                                + externalFile, e);
            }
        }

        // environment variables to win over both files
        for (String key : KEYS) {
            String value = environment.apply(variableName(key));
            if (value != null && !value.isEmpty()) {
                settings.setProperty(key, value);
            }
        }

        // a stray space after a value (e.g. "db.username=Green ") would otherwise be part of
        // the user name; the password is left exactly as typed
        for (String key : KEYS) {
            String value = settings.getProperty(key);
            if (value != null && !key.equals("db.password")) {
                settings.setProperty(key, value.trim());
            }
        }

        // db.user and db.username have both been used; make either one work
        copyIfMissing(settings, "db.username", "db.user");
        copyIfMissing(settings, "db.user", "db.username");

        // no full url given: build one from host / port / name corrercted using Gemini AI here 
        if (isBlank(settings.getProperty("db.url")) && !isBlank(settings.getProperty("db.host"))) {
            settings.setProperty("db.url",
                    "jdbc:mysql://" + settings.getProperty("db.host").trim()
                            + ":" + settings.getProperty("db.port", "3306").trim()
                            + "/" + settings.getProperty("db.name", "moffat_bay").trim()
                            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }

        return settings;
    }

    // db.url -> MARINA_DB_URL, db.password -> MARINA_DB_PASSWORD, db.user/db.username -> MARINA_DB_USER
	// This section was added as per suggestion of Gemini when looking over way this file was causing issues 
    static String variableName(String key) {
        if (key.equals("db.user") || key.equals("db.username")) {
            return "MARINA_DB_USER";
        }
        return "MARINA_" + key.replace('.', '_').toUpperCase();
    }

    private static void copyIfMissing(Properties settings, String missing, String source) {
        if (isBlank(settings.getProperty(missing)) && !isBlank(settings.getProperty(source))) {
            settings.setProperty(missing, settings.getProperty(source));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
