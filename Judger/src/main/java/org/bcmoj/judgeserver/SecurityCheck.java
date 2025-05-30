package org.bcmoj.judgeserver;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SecurityCheck {
    public static Logger LOGGER = LoggerFactory.getLogger(SecurityCheck.class);

    private static final String REGEX_PREFIX = "regex:";

    public static int CodeSecurityCheck(File fileName ,File keywordsFile) {
        List<Pattern> keywordPatterns = loadKeywords(keywordsFile);
        if (keywordPatterns.isEmpty()) {
            LOGGER.error("No security keywords loaded");
            return -5;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                for (Pattern pattern : keywordPatterns) {
                    if (pattern.matcher(line).find()) {
                        LOGGER.warn("Dangerous pattern detected: '{}' in line {}", pattern.pattern(), lineNumber);
                        return -5;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read file: {}", fileName, e);
            return -5;
        }
        LOGGER.info("Security check passed");
        return 0;
    }
    private static List<Pattern> loadKeywords(File keywordsFile) {
        List<Pattern> patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(keywordsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    if (line.startsWith(REGEX_PREFIX)) {
                        String regex = line.substring(REGEX_PREFIX.length()).trim();
                        try {
                            patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                            LOGGER.debug("Loaded regex pattern: {}", regex);
                        } catch (Exception e) {
                            LOGGER.warn("Invalid regex pattern: {}, skipped", regex);
                        }
                    } else {
                        patterns.add(Pattern.compile("\\b" + Pattern.quote(line) + "\\b", Pattern.CASE_INSENSITIVE));
                    }
                }
            }
            LOGGER.info("Loaded {} keyword patterns ({} regex)", patterns.size(),
                    patterns.stream().filter(p -> p.pattern().startsWith("\\b")).count());
        } catch (IOException e) {
            LOGGER.error("Failed to read keywords file", e);
        }
        return patterns;
    }
}