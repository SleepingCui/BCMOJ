package org.bcmoj.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GetConfig {
    public static Logger LOGGER = LoggerFactory.getLogger(GetConfig.class);
    public static String getProperties(String key) {
        Properties properties = new Properties();
        String value = null;

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
            value = properties.getProperty(key);
        } catch (IOException e) {
            LOGGER.error("Error reading config file", e);
        }

        return value;
    }

}
