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

public class JudgeServer {
    public static Logger LOGGER = LoggerFactory.getLogger(JudgeServer.class);
    public static class Config {
        public int timeLimit;
        public JsonNode checkpoints;
        public boolean securityCheck;
    }
    public static String JServer(String jsonConfig, File cppFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Config config = mapper.readValue(jsonConfig, Config.class);
            LOGGER.debug(String.valueOf(config.securityCheck));
            if (config.securityCheck) {
                int securityCheckResult = SecurityCheck.CodeSecurityCheck(cppFilePath);
                if (securityCheckResult == -5) {
                    return buildJudgeResult(new ArrayList<>(), true);
                }
            }
            JsonNode checkpoints = config.checkpoints;
            int checkpointsCount = checkpoints.size() / 2;
            ExecutorService executor = Executors.newFixedThreadPool(checkpointsCount);
            List<Future<Judger.JudgeResult>> futures = new ArrayList<>();
            // 生成检查点
            for (int i = 1; i <= checkpointsCount; i++) {
                String inputKey = i + "_in";
                String outputKey = i + "_out";
                String inputContent = checkpoints.get(inputKey).asText();
                String outputContent = checkpoints.get(outputKey).asText();

                Callable<Judger.JudgeResult> task = () -> Judger.judge(cppFilePath, inputContent, outputContent, config.timeLimit);
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
                LOGGER.info("Checkpoint {} result: {} ({}), Time: {}ms", i + 1, result.statusCode, status, result.timeMs);
            }
            return buildJudgeResult(results,false);

        } catch (Exception e) {
            LOGGER.error("Failed to execute judge tasks: {}", e.getMessage());
            return "{}";
        }
    }
    private static String buildJudgeResult(List<Judger.JudgeResult> results, boolean isSecurityCheckFailed) {
        StringBuilder jsonResult = new StringBuilder();
        jsonResult.append("{");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                jsonResult.append(",");
            }
            if (isSecurityCheckFailed) {
                jsonResult.append("\"").append(i + 1).append("_res\":").append(-5)
                        .append(",\"").append(i + 1).append("_time\":").append(0);
            } else {
                Judger.JudgeResult result = results.get(i);
                jsonResult.append("\"").append(i + 1).append("_res\":").append(result.statusCode)
                        .append(",\"").append(i + 1).append("_time\":").append(result.timeMs);
            }
        }
        jsonResult.append("}");
        return jsonResult.toString();
    }
    private static String getStatusDescription(int statusCode) {
        return switch (statusCode) {
            case -4 -> "Compile Error";
            case -3 -> "Wrong Answer";
            case 2 -> "Real Time Limit Exceeded";
            case 4 -> "Runtime Error";
            case 5 -> "System Error";
            case 1 -> "Accepted";
            case -5 -> "Security Check Failed";
            default -> "Unknown Status";
        };
    }
}