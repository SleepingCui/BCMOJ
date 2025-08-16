package org.bcmoj.bootstrap;

import java.util.Properties;

/**
 * Utility class for parsing command-line arguments into {@link Properties}.
 * <p>
 * Supports arguments in the form of {@code --key=value}. Arguments without '=' are ignored.
 * </p>
 *
 * @author SleepingCui
 */
public class CommandParser {
    public static Properties parse(String[] args) {
        Properties props = new Properties();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    props.setProperty(parts[0], parts[1]);
                }
            }
        }
        return props;
    }
}
