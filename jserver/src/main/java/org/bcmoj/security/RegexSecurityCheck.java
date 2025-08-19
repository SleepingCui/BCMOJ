package org.bcmoj.security;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security checker that scans source code files for dangerous keywords or patterns.
 * Supports both literal keywords and regular expressions prefixed with "regex:".
 * <p>
 * The class reads rules from a rule file and checks each line of the source file.
 * If any dangerous pattern is detected, the check fails immediately.
 * </p>
 *
 * @author BCMOJ
 * @version ${project.version}
 */
@Slf4j
public class RegexSecurityCheck implements SecurityChecker {

    private static final String REGEX_PREFIX = "regex:";

    /**
     * Performs the security check on a given source file using the provided rule file.
     *
     * @param sourceFile The source code file to check
     * @param ruleFile   The rule file containing keywords or regex patterns
     * @return 0 if passed, -5 if any dangerous pattern is detected or error occurs
     */
    @Override
    public int check(File sourceFile, File ruleFile) {
        List<Pattern> keywordPatterns = loadKeywords(ruleFile);
        if (keywordPatterns.isEmpty()) {
            log.error("No security keywords loaded");
            return -5;
        }
        return scanSourceFile(sourceFile, keywordPatterns);
    }

    /**
     * Loads keyword patterns from the rule file.
     * Lines starting with "regex:" are treated as regex patterns,
     * other lines are treated as literal keywords.
     *
     * @param ruleFile The rule file
     * @return List of compiled Patterns
     */
    private List<Pattern> loadKeywords(File ruleFile) {
        List<Pattern> patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(ruleFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                Pattern pattern = parseLineToPattern(line);
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read keywords file '{}'", ruleFile, e);
        }
        return patterns;
    }

    /**
     * Parses a single line from the rule file into a Pattern.
     *
     * @param line keyword or regex line
     * @return compiled Pattern or null if invalid
     */
    private Pattern parseLineToPattern(String line) {
        if (line.startsWith(REGEX_PREFIX)) {
            String regex = line.substring(REGEX_PREFIX.length()).trim();
            try {
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                log.debug("Loaded regex pattern: {}", regex);
                return pattern;
            } catch (Exception e) {
                log.warn("Invalid regex pattern: {}, skipped", regex);
                return null;
            }
        } else {
            return Pattern.compile("\\b" + Pattern.quote(line) + "\\b", Pattern.CASE_INSENSITIVE);
        }
    }

    /**
     * Scans the source file line by line against all keyword patterns.
     *
     * @param sourceFile     The source code file
     * @param keywordPatterns List of compiled Patterns
     * @return 0 if passed, -5 if any dangerous pattern is detected
     */
    private int scanSourceFile(File sourceFile, List<Pattern> keywordPatterns) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (matchesAnyPattern(line, keywordPatterns)) {
                    return -5;
                }
            }
        } catch (IOException e) {
            log.error("Failed to read file '{}'", sourceFile, e);
            return -5;
        }
        log.info("Security check passed");
        return 0;
    }

    /**
     * Checks whether the line matches any pattern in the list.
     *
     * @param line            The line to check
     * @param keywordPatterns List of compiled Patterns
     * @return true if any pattern matches, false otherwise
     */
    private boolean matchesAnyPattern(String line, List<Pattern> keywordPatterns) {
        for (Pattern pattern : keywordPatterns) {
            if (pattern.matcher(line).find()) {
                log.warn("Dangerous pattern detected: '{}' in line '{}'", pattern.pattern(), line);
                return true;
            }
        }
        return false;
    }
}
