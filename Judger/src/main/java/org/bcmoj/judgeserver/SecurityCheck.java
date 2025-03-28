package org.bcmoj.judgeserver;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class SecurityCheck {
    public static Logger LOGGER = LoggerFactory.getLogger(SecurityCheck.class);

    private static final String[] KEYWORDS = {
            "system", "exec", "fork", "popen", "pclose", "chmod", "chown",
            "rmdir", "unlink", "kill", "shutdown", "reboot", "sudo", "su","rm"
    };
    public static int CodeSecurityCheck(File fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                for (String keyword : KEYWORDS) {
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
}