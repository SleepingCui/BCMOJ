package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class ConfigLoader {

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
