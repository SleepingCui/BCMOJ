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
            properties.setProperty("ServerPort", "12345");
            properties.setProperty("ServerIP", "0.0.0.0");
            properties.setProperty("KeywordsFilePath", "keywords.txt");
            properties.store(outputStream, "JudgeServer Configuration");

        } catch (IOException e) {
            LOGGER.error("Error writing properties file", e);

        }
    }

}