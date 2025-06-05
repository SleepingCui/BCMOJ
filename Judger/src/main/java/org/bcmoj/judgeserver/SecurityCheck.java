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
/**
 * Code Security Checker
 * <p>
 * Performs static analysis on submitted code to detect dangerous patterns and restricted keywords.
 * Supports both literal keyword matching and regular expression patterns.
 * </p>
 *
 * <p><b>Security Check Process:</b></p>
 * <ol>
 *   <li>Loads keywords/patterns from configuration file</li>
 *   <li>Scans each line of submitted code</li>
 *   <li>Matches against restricted patterns (case-insensitive)</li>
 *   <li>Returns security violation status (-5) if any matches found</li>
 * </ol>
 *
 * <p><b>Keywords File Format:</b></p>
 * <pre>
 * # Comments start with #
 * system             # Literal keyword match
 * exec               # Another literal keyword
 * regex:fork\\s*\\(  # Regular expression pattern
 * regex:exec[lv]p\\b # Another regex pattern
 * </pre>
 *
 * <p><b>Return Codes:</b></p>
 * <ul>
 *   <li>0: Security check passed</li>
 *   <li>-5: Security violation detected</li>
 * </ul>
 *
 * <p><b>Pattern Matching:</b></p>
 * <ul>
 *   <li>Literal keywords are matched as whole words</li>
 *   <li>Regex patterns (prefixed with <code>regex:</code>) are compiled as-is</li>
 *   <li>All matching is case-insensitive</li>
 * </ul>
 *
 * @author SleepingCui
 * @version 1.0-SNAPSHOT
 * @since 2025
 * @see <a href="https://github.com/SleepingCui/bcmoj-judge-server">GitHub Repository</a>
 */
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