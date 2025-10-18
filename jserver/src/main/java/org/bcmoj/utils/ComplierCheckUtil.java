package org.bcmoj.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility class for checking the version of the g++ compiler.
 */
public class ComplierCheckUtil {

    /**
     * Gets the version of the g++ compiler from the system PATH.
     *
     * @return the g++ version string (e.g., "12.3.0"), or null if not found.
     */
    public static String getGppVersion() {
        return getGppVersion("g++");
    }

    /**
     * Gets the version of the g++ compiler from a specified path.
     *
     * @param path the full path to the g++ executable, or "g++" to use the system PATH
     * @return the g++ version string (e.g., "12.3.0"), or null if not found or failed to execute
     */
    public static String getGppVersion(String path) {
        String version = run(path, "-dumpfullversion");
        if (version == null || version.isBlank()) {
            version = run(path, "-dumpversion");
        }
        return version != null ? version.trim() : null;
    }

    private static String run(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            return line;
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
