package org.bcmoj.quesmm.cfg;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GetConfig {
    public static String getProperties(String key) {
        Properties properties = new Properties();
        String value = null;

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
            value = properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

}
