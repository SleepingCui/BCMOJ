package org.bcmoj.judgeserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Judger;
import org.bcmoj.security.RegexSecurityCheck;
import org.bcmoj.security.SecurityChecker;
import org.bcmoj.utils.JudgeResultUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class JudgeServer {

    public static class Config {
        public int timeLimit;
        public JsonNode checkpoints;
        public boolean securityCheck;
    }

    public static String JServer(String jsonConfig, File cppFilePath, File keywordsFilePath) {
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
                    log.error(e.getMessage());
                    results.add(new Judger.JudgeResult(5, 0.0));
                }
            }
            log.info("========== Results ==========");
            for (int i = 0; i < results.size(); i++) {
                Judger.JudgeResult result = results.get(i);
                String status = getStatusDescription(result.statusCode);
                log.info("Checkpoint {} result: {} ({}), Time: {}ms", i + 1, result.statusCode, status, result.time);
            }
            return JudgeResultUtil.buildResult(results, securityCheckFailed, false, checkpointsCount);
        } catch (Exception e) {
            log.error("Failed to execute judge tasks: {}", e.getMessage());
            return JudgeResultUtil.buildResult(null, false, true, 0);
        }
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
