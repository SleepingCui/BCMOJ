package org.bcmoj.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class initConfig {
    public static Logger LOGGER = LoggerFactory.getLogger(initConfig.class);

    public static void writeProperties() {
        Properties properties = new Properties();

        try (OutputStream outputStream = new FileOutputStream("config.properties")) {
            properties.setProperty("ServerPort", "12402");
            properties.setProperty("KeywordsFilePath", "keywords.txt");
            properties.store(outputStream, "Judger Configuration");

        } catch (IOException e) {
            LOGGER.error("Error writing properties file", e);

        }
    }

}