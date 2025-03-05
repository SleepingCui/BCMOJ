package org.bcmoj.quesmm.cfg;

import java.io.*;
import java.util.Properties;

public class WriteConfig {
    public static void writeProperties() {
        Properties properties = new Properties();
        try {
            OutputStream outputStream;
            try {
                outputStream = new FileOutputStream("config.properties");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            properties.setProperty("db_questions_user", "root");
            properties.setProperty("db_questions_password", "yourpassword");
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
