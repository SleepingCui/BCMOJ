package org.bcmoj.utils;

import java.io.*;
import java.util.Properties;

/**
 * Provides configuration management utilities for the BCMOJ Judge Server.
 * <p>
 * This class handles loading, accessing, and updating configuration properties
 * from a properties file. It supports both in-memory configuration updates
 * and persistent storage to disk. The class is designed as a singleton with
 * static methods for easy access throughout the application.
 * </p>
 *
 * <p><b>Default Configuration:</b></p>
 * <ul>
 *   <li><code>ServerPort=8080</code> - Default server port</li>
 *   <li><code>ServerIP=0.0.0.0</code> - Default server binding address</li>
 *   <li><code>KeywordsFile=keywords.txt</code> - Default keywords file path</li>
 * </ul>
 *
 * <p><b>File Location:</b> The configuration file is expected to be named
 * <code>config.properties</code> in the application's working directory.</p>
 *
 * @author MxingFoew1034
 * @version 1.0
 * @since 2025
 * @see Properties
 */
public class ConfigProcess {
    private static final String CONFIG_FILE = "config.properties";
    private static Properties props = null;

    /**
     * Ensures configuration is loaded from disk if not already loaded.
     * Loads default values if the config file is missing or invalid.
     */
    private static void ensureLoaded() {
        if (props != null) return;

        props = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
        } catch (IOException e) {
            System.err.println("[ensureLoad]Error loading config file: " + e.getMessage());
            setDefaultConfig();
        }
    }

    /**
     * Initializes default configuration values.
     * <p>
     * These values are used when the configuration file is not available.
     * </p>
     */
    private static void setDefaultConfig() {
        props.setProperty("ServerPort", "8080");
        props.setProperty("ServerIP", "0.0.0.0");
        props.setProperty("KeywordsFile", "keywords.txt");
    }

    /**
     * Retrieves a configuration value by key.
     *
     * @param key The configuration key to retrieve
     * @return The configuration value, or null if the key doesn't exist
     */
    public static String GetConfig(String key) {
        ensureLoaded();
        return props.getProperty(key);
    }

    /**
     * Updates a configuration value in memory.
     * <p>
     * Note: This change is not persisted to disk. Use {@link #UpdateAndSaveConfig}
     * for persistent changes.
     * </p>
     *
     * @param key The configuration key to update
     * @param value The new value for the configuration key
     */
    public static void UpdateConfig(String key, String value) {
        ensureLoaded();
        props.setProperty(key, value);
    }

    /**
     * Updates a configuration value and persists it to disk.
     *
     * @param key The configuration key to update
     * @param value The new value for the configuration key
     * @throws IOException If the configuration file cannot be written
     */
    public static void UpdateAndSaveConfig(String key, String value) throws IOException {
        UpdateConfig(key, value);
        saveConfig();
    }

    /**
     * Saves the current configuration to disk.
     *
     * @throws IOException If the configuration file cannot be written
     */
    public static void saveConfig() throws IOException {
        ensureLoaded();
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "Updated configuration");
        }
    }

    /**
     * Reloads configuration from disk.
     * <p>
     * This clears all in-memory configuration and reloads from the properties file.
     * </p>
     */
    public static void reloadConfig() {
        props = null;
        ensureLoaded();
    }
}
