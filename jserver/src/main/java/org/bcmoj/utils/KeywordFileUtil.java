package org.bcmoj.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for writing keyword files used by BCMOJ Judge Server.
 * <p>
 * This class provides methods to create and write security check keyword files
 * that contain dangerous system commands and functions that should be blocked
 * during code execution.
 * </p>
 *
 * @author MxingFoew1034
 * @version 1.0
 * @since 2025
 */
public class KeywordFileUtil {
    /**
     * Default security keywords that should be blocked
     */
    private static final List<String> DEFAULT_KEYWORDS = Arrays.asList(
            "system",
            "exec",
            "fork",
            "popen",
            "pclose",
            "chmod",
            "chown",
            "rmdir",
            "unlink",
            "kill",
            "shutdown",
            "reboot",
            "sudo",
            "su",
            "rm"
    );

    /**
     * Default header comment for the keywords file
     */
    private static final String DEFAULT_HEADER = """
            # Security check keywords list
            # Lines starting with # are ignored""";

    /**
     * Writes the default keyword list to the specified file path.
     *
     * @param filePath the path where the keywords file should be created
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static void writeDefaultKeywords(String filePath) throws IOException {
        writeKeywords(filePath, DEFAULT_KEYWORDS, DEFAULT_HEADER);
    }

    /**
     * Writes a custom keyword list to the specified file path.
     *
     * @param filePath the path where the keywords file should be created
     * @param keywords list of keywords to write
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static void writeKeywords(String filePath, List<String> keywords) throws IOException {
        writeKeywords(filePath, keywords, DEFAULT_HEADER);
    }

    /**
     * Writes a custom keyword list with custom header to the specified file path.
     *
     * @param filePath the path where the keywords file should be created
     * @param keywords list of keywords to write
     * @param header   custom header comment for the file
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static void writeKeywords(String filePath, List<String> keywords, String header) throws IOException {
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            if (header != null && !header.trim().isEmpty()) {
                writer.write(header);
                writer.newLine();
            }
            for (String keyword : keywords) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    writer.write(keyword.trim());
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Creates a keywords file with default content if it doesn't exist.
     *
     * @param filePath the path where the keywords file should be created
     * @return true if the file was created, false if it already exists
     * @throws IOException if an I/O error occurs while creating the file
     */
    public static boolean createDefaultIfNotExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (Files.exists(path)) {
            return false;
        }

        writeDefaultKeywords(filePath);
        return true;
    }

    /**
     * Appends additional keywords to an existing keywords file.
     *
     * @param filePath           the path to the existing keywords file
     * @param additionalKeywords list of keywords to append
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static void appendKeywords(String filePath, List<String> additionalKeywords) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.newLine(); // Add a blank line before new keywords
            writer.write("# Additional keywords");
            writer.newLine();

            for (String keyword : additionalKeywords) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    writer.write(keyword.trim());
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Gets the default keywords list.
     *
     * @return a copy of the default keywords list
     */
    public static List<String> getDefaultKeywords() {
        return List.copyOf(DEFAULT_KEYWORDS);
    }
}