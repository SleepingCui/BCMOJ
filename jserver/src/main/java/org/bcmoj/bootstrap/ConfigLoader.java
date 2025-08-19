package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for loading and merging configuration properties.
 * <p>
 * This class provides methods to load configuration values from a
 * specified file path and to merge multiple {@link Properties} objects,
 * with override precedence applied.
 * </p>
 *
 * @author SleepingCui
 */
@Slf4j
public class ConfigLoader {

    /**
     * Loads configuration properties from a given file path.
     * <p>
     * If the file path is {@code null}, this method returns an empty
     * {@link Properties} object.
     * </p>
     *
     * @param configFilePath the path to the configuration file, may be {@code null}
     * @return a {@link Properties} object containing loaded key-value pairs;
     *         empty if the file path is {@code null}
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static Properties loadConfig(String configFilePath) throws IOException {
        Properties props = new Properties();
        if (configFilePath != null) {
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                props.load(fis);
                log.info("Loaded config file: {}", configFilePath);
            }
        }
        return props;
    }

    /**
     * Merges two sets of properties into a new {@link Properties} object.
     * <p>
     * If duplicate keys exist, the values from {@code overrideProps} take
     * precedence over those in {@code baseProps}.
     * </p>
     *
     * @param baseProps     the base properties, may be {@code null}
     * @param overrideProps the properties to override with, may be {@code null}
     * @return a new {@link Properties} instance containing merged properties
     */
    public static Properties merge(Properties baseProps, Properties overrideProps) {
        Properties merged = new Properties();
        if (baseProps != null) {
            merged.putAll(baseProps);
        }
        if (overrideProps != null) {
            overrideProps.forEach((k, v) -> merged.setProperty(k.toString(), v.toString()));
        }
        return merged;
    }
}
