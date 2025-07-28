package org.bcmoj.judgeserver.security;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class RegexSecurityCheck implements SecurityChecker {

    private static final String REGEX_PREFIX = "regex:";

    @Override
    public int check(File sourceFile, File ruleFile) {
        List<Pattern> keywordPatterns = loadKeywords(ruleFile);
        if (keywordPatterns.isEmpty()) {
            log.error("No security keywords loaded");
            return -5;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                for (Pattern pattern : keywordPatterns) {
                    if (pattern.matcher(line).find()) {
                        log.warn("Dangerous pattern detected: '{}' in line {}", pattern.pattern(), lineNumber);
                        return -5;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to read file: {}", sourceFile, e);
            return -5;
        }
        log.info("Security check passed");
        return 0;
    }

    private List<Pattern> loadKeywords(File ruleFile) {
        List<Pattern> patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(ruleFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    if (line.startsWith(REGEX_PREFIX)) {
                        String regex = line.substring(REGEX_PREFIX.length()).trim();
                        try {
                            patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                            log.debug("Loaded regex pattern: {}", regex);
                        } catch (Exception e) {
                            log.warn("Invalid regex pattern: {}, skipped", regex);
                        }
                    } else {
                        patterns.add(Pattern.compile("\\b" + Pattern.quote(line) + "\\b", Pattern.CASE_INSENSITIVE));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to read keywords file", e);
        }
        return patterns;
    }
}
