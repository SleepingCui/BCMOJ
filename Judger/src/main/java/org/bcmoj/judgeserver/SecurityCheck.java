package org.bcmoj.judgeserver;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SecurityCheck {
    public static Logger LOGGER = LoggerFactory.getLogger(SecurityCheck.class);

    private static final String[] DEFAULT_KEYWORDS = {
            "system", "exec", "fork", "popen", "pclose", "chmod", "chown",
            "rmdir", "unlink", "kill", "shutdown", "reboot", "sudo", "su", "rm"
    };
    private static final String KEYWORDS_FILE = "keywords.txt";

    public static int CodeSecurityCheck(File fileName) {
        List<String> keywords = loadKeywords();
        if (keywords.isEmpty()) {
            LOGGER.error("No security keywords loaded");
            return -5;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                for (String keyword : keywords) {
                    if (containsKeyword(line, keyword)) {
                        LOGGER.warn("Dangerous keyword: '{}' in line {}", keyword, lineNumber);
                        return -5;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read file {}", fileName, e);
            return -5;
        }
        LOGGER.info("Security check passed");
        return 0;
    }
    private static boolean containsKeyword(String line, String keyword) {
        return Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(line)
                .find();
    }
    private static List<String> loadKeywords() {
        List<String> keywords = new ArrayList<>();
        File keywordsFile = new File(KEYWORDS_FILE);
        if (!keywordsFile.exists()) {
            try (FileWriter writer = new FileWriter(keywordsFile)) {
                writer.write("# Security check keywords list" + System.lineSeparator());
                writer.write("# Lines starting with # are ignored" + System.lineSeparator());
                writer.write(System.lineSeparator());
                for (String keyword : DEFAULT_KEYWORDS) {
                    writer.write(keyword + System.lineSeparator());
                }
                LOGGER.info("Created default keywords file");
            } catch (IOException e) {
                LOGGER.error("Failed to create keywords file", e);
                return keywords;
            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(keywordsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) { //忽略#
                    keywords.add(line);
                }
            }
            LOGGER.info("Loaded {} keywords", keywords.size());
        } catch (IOException e) {
            LOGGER.error("Failed to read keywords file", e);
        }
        return keywords;
    }
}