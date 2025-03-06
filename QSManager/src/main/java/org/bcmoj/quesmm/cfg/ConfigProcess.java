package org.bcmoj.quesmm.cfg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static java.lang.System.exit;

public class ConfigProcess {
    public static Logger LOGGER = LoggerFactory.getLogger(ConfigProcess.class);
    public static String ConfigProcess(String key) {
        File ConfigFilePath = new File("config.properties");
        if (ConfigFilePath.exists()) {
            LOGGER.info("Config file not exist! Generating new one...");
            WriteConfig.writeProperties();
            exit(0);

        }
        else {
            return GetConfig.getProperties(key);
        }
        return null;
    }

}
