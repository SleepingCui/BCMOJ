package org.bcmoj.judgeserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Judger;
import org.bcmoj.security.RegexSecurityCheck;
import org.bcmoj.security.SecurityChecker;
import org.bcmoj.utils.JudgeResultUtil;
import org.bcmoj.utils.OutputCompareUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * JudgeServer handles execution of submitted C++ programs against multiple test cases (checkpoints),
 * optionally performing security checks and output comparison.
 *
 * <p>This class parses a JSON configuration containing time limits, checkpoints, security check flag,
 * optimization flag, and output comparison mode. It executes each test case concurrently and
 * aggregates the results.</p>
 *
 * <p>Security check uses {@link SecurityChecker} implementation {@link RegexSecurityCheck}.</p>
 *
 * <p>Status codes returned by {@link Judger}:</p>
 * <ul>
 *     <li>-5: Security Check Failed</li>
 *     <li>-4: Compile Error</li>
 *     <li>-3: Wrong Answer</li>
 *     <li>2: Real Time Limit Exceeded</li>
 *     <li>4: Runtime Error</li>
 *     <li>5: System Error</li>
 *     <li>1: Accepted</li>
 * </ul>
 *
 * <p>Output comparison modes:</p>
 * <ul>
 *     <li>1: STRICT (default)</li>
 *     <li>2: IGNORE_SPACES</li>
 *     <li>3: CASE_INSENSITIVE</li>
 *     <li>4: FLOAT_TOLERANT</li>
 * </ul>
 *
 * @author SleepingCui
 */
@Slf4j
public class JudgeServer {

    public static class Config {
        public int timeLimit;
        public JsonNode checkpoints;
        public boolean securityCheck;
        public boolean enableO2;
        public int compareMode = 1;
    }

    /**
     * Serves judging requests for a C++ program.
     *
     * <p>It parses the JSON configuration, optionally runs security check, executes the program
     * on each checkpoint concurrently, and aggregates results into a JSON string.</p>
     *
     * @param jsonConfig       JSON string containing checkpoints, time limits, and flags
     * @param cppFilePath      path to the submitted C++ source file
     * @param keywordsFilePath path to the keyword file used for security check
     * @return JSON string representing aggregated judge results, including checkpoint results,
     *         security check status, and system error flag
     */
    public static String serve(String jsonConfig, File cppFilePath, File keywordsFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Config config = mapper.readValue(jsonConfig, Config.class);
            JsonNode checkpoints = config.checkpoints;
            int checkpointsCount = checkpoints.size() / 2;
            ExecutorService executor = Executors.newFixedThreadPool(checkpointsCount);
            List<Future<Judger.JudgeResult>> futures = new ArrayList<>();

            boolean securityCheckFailed;
            if (config.securityCheck) {
                SecurityChecker checker = new RegexSecurityCheck();
                int securityCheckResult = checker.check(cppFilePath, keywordsFilePath);
                securityCheckFailed = (securityCheckResult == -5);
            } else {
                securityCheckFailed = false;
                log.info("Code Security Check is not enabled");
            }

            OutputCompareUtil.CompareMode mode = switch (config.compareMode) {
                case 2 -> OutputCompareUtil.CompareMode.IGNORE_SPACES;
                case 3 -> OutputCompareUtil.CompareMode.CASE_INSENSITIVE;
                case 4 -> OutputCompareUtil.CompareMode.FLOAT_TOLERANT;
                default -> OutputCompareUtil.CompareMode.STRICT;
            };

            for (int i = 1; i <= checkpointsCount; i++) {
                String inputContent = checkpoints.get(i + "_in").asText();
                String outputContent = checkpoints.get(i + "_out").asText();
                Task task = new Task(cppFilePath, inputContent, outputContent, config.timeLimit, config.enableO2, mode, securityCheckFailed);
                futures.add(executor.submit(task));
            }

            executor.shutdown();
            List<Judger.JudgeResult> results = new ArrayList<>();
            for (Future<Judger.JudgeResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                    results.add(new Judger.JudgeResult(5, 0.0));
                }
            }
            log.info("========== Results ==========");
            for (int i = 0; i < results.size(); i++) {
                Judger.JudgeResult result = results.get(i);
                log.info("Checkpoint {} result: {} ({}), Time: {}ms", i + 1, result.statusCode, StatusDescription(result.statusCode), result.time);
            }
            return JudgeResultUtil.buildResult(results, securityCheckFailed, false, checkpointsCount);
        } catch (Exception e) {
            log.error("Failed to execute judge tasks: {}", e.getMessage());
            return JudgeResultUtil.buildResult(null, false, true, 1);
        }
    }

    private static String StatusDescription(int statusCode) {
        return switch (statusCode) {
            case -5 -> "Security Check Failed";
            case -4 -> "Compile Error";
            case -3 -> "Wrong Answer";
            case 2 -> "Real Time Limit Exceeded";
            case 4 -> "Runtime Error";
            case 5 -> "System Error";
            case 1 -> "Accepted";
            default -> "Unknown Status";
        };
    }
}
