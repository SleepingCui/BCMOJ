package org.bcmoj.bootstrap;

import java.util.Properties;

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
