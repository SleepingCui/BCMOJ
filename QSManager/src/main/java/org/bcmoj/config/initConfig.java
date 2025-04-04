package org.bcmoj.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class initConfig {
    public static Logger LOGGER = LoggerFactory.getLogger(initConfig.class);

    public static void writeProperties() {
        Properties properties = new Properties();
        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream("config.properties");
            properties.setProperty("db_user", "root");
            properties.setProperty("db_password", "secret");
            properties.setProperty("db_port", "3306");
            properties.setProperty("db_host", "localhost");
            properties.setProperty("enableCodeSecurityCheck","false");
            properties.store(outputStream, "Judge Configuration");

        } catch (IOException e) {
            LOGGER.error("Error writing properties file", e);

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing output stream", e);
                }
            }
        }
    }

}