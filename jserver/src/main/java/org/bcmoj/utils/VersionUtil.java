package org.bcmoj.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for retrieving the application version from a properties file.
 * <p>
 * This class loads the version from {@code version.properties} located in the classpath,
 * typically generated during the Maven build process via resource filtering.
 */
@Slf4j
public class VersionUtil {
    public static String getVersion() {
        try (InputStream in = VersionUtil.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                return props.getProperty("version", "UNKNOWN");
            } else {
                log.warn("version.properties not found in classpath.Make sure you are using the latest version.");
            }
        }
        catch (IOException e) {
        log.debug("Failed to read version.properties. Make sure you are using the latest version. \nDetails: {}", e.toString());
        }
        return "UNKNOWN";
    }
}
