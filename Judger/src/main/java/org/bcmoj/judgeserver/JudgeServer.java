package org.bcmoj.judgeserver;

import org.bcmoj.judger.Judger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
/**
 * Judge Server Implementation
 * <p>
 * Core component that coordinates the execution of code submissions against multiple test cases.
 * Manages parallel test case evaluation and compiles final results.
 * </p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Parallel test case execution using thread pool</li>
 *   <li>Optional code security validation</li>
 *   <li>Timeout handling for each test case</li>
 *   <li>Comprehensive result aggregation</li>
 * </ul>
 *
 * <p><b>Execution Flow:</b></p>
 * <ol>
 *   <li>Parse JSON configuration (timeLimit, securityCheck, checkpoints)</li>
 *   <li>Perform security check if enabled</li>
 *   <li>Execute all test cases in parallel</li>
 *   <li>Aggregate results into JSON response</li>
 * </ol>
 *
 * <p><b>Result Codes:</b></p>
 * <ul>
 *   <li>-5: Security Check Failed</li>
 *   <li>-4: Compile Error</li>
 *   <li>-3: Wrong Answer</li>
 *   <li>2: Real Time Limit Exceeded</li>
 *   <li>4: Runtime Error</li>
 *   <li>5: System Error</li>
 *   <li>1: Accepted</li>
 * </ul>
 *
 * <p><b>Response Format:</b></p>
 * <pre>{
 *   "1_res": [status_code],
 *   "1_time": [execution_time],
 *   ...
 * }</pre>
 *
 * @author SleepingCui
 * @version 1.0-SNAPSHOT
 * @since 2025
 * @see <a href="https://github.com/SleepingCui/bcmoj-judge-server">GitHub Repository</a>
 */
public class JudgeServer {
    public static Logger LOGGER = LoggerFactory.getLogger(JudgeServer.class);
    public static class Config {
        public int timeLimit;
        public JsonNode checkpoints;
        public boolean securityCheck;
    }
    public static String JServer(String jsonConfig, File cppFilePath,File keywordsFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Config config = mapper.readValue(jsonConfig, Config.class);
            JsonNode checkpoints = config.checkpoints;
            int checkpointsCount = checkpoints.size() / 2;
            ExecutorService executor = Executors.newFixedThreadPool(checkpointsCount);
            List<Future<Judger.JudgeResult>> futures = new ArrayList<>();
            boolean securityCheckFailed;
            if (config.securityCheck) {
                int securityCheckResult = SecurityCheck.CodeSecurityCheck(cppFilePath,keywordsFilePath);
                securityCheckFailed = (securityCheckResult == -5);
            }
            else {
                securityCheckFailed = false;
                LOGGER.info("Code Security Check is not enabled");
            }
            // 生成检查点
            for (int i = 1; i <= checkpointsCount; i++) {
                final int checkpointIndex = i;
                Callable<Judger.JudgeResult> task = () -> {
                    if (securityCheckFailed) {
                        return new Judger.JudgeResult(-5, 0.0);
                    } else {
                        String inputKey = checkpointIndex + "_in";
                        String outputKey = checkpointIndex + "_out";
                        String inputContent = checkpoints.get(inputKey).asText();
                        String outputContent = checkpoints.get(outputKey).asText();
                        return Judger.judge(cppFilePath, inputContent, outputContent, config.timeLimit);
                    }
                };
                Future<Judger.JudgeResult> future = executor.submit(task);
                futures.add(future);
            }
            executor.shutdown();
            List<Judger.JudgeResult> results = new ArrayList<>();
            for (Future<Judger.JudgeResult> future : futures) {
                try {
                    Judger.JudgeResult result = future.get();
                    results.add(result);
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error(e.getMessage());
                    results.add(new Judger.JudgeResult(5, 0.0));
                }
            }
            LOGGER.info("========== Results ==========");
            for (int i = 0; i < results.size(); i++) {
                Judger.JudgeResult result = results.get(i);
                String status = getStatusDescription(result.statusCode);
                LOGGER.info("Checkpoint {} result: {} ({}), Time: {}ms", i + 1, result.statusCode, status, result.time);
            }
            return buildJudgeResult(results, securityCheckFailed, false, checkpointsCount);
        } catch (Exception e) {
            LOGGER.error("Failed to execute judge tasks: {}", e.getMessage());
            return buildJudgeResult(null, false, true, 0);
        }
    }
    private static String buildJudgeResult(List<Judger.JudgeResult> results,boolean isSecurityCheckFailed,boolean isSystemError,int checkpointsCount) {
        StringBuilder jsonResult = new StringBuilder();
        jsonResult.append("{");
        int actualCount = isSecurityCheckFailed || isSystemError ? checkpointsCount : (results != null ? results.size() : 0);
        for (int i = 0; i < actualCount; i++) {
            if (i > 0) {
                jsonResult.append(",");
            }
            if (isSecurityCheckFailed) {
                jsonResult.append("\"").append(i + 1).append("_res\":").append(-5)
                        .append(",\"").append(i + 1).append("_time\":").append(0.0);
            }
            else if (isSystemError) {
                jsonResult.append("\"").append(i + 1).append("_res\":").append(5)
                        .append(",\"").append(i + 1).append("_time\":").append(0.0);
            }
            else {
                Judger.JudgeResult result = results.get(i);
                jsonResult.append("\"").append(i + 1).append("_res\":").append(result.statusCode)
                        .append(",\"").append(i + 1).append("_time\":").append(result.time);
            }
        }
        jsonResult.append("}");
        return jsonResult.toString();
    }
    private static String getStatusDescription(int statusCode) {
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