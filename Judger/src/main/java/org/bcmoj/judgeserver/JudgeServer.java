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

    // 状态码
    public static final int COMPILE_ERROR = -4;
    public static final int WRONG_ANSWER = -3;
    public static final int REAL_TIME_LIMIT_EXCEEDED = 2;
    public static final int RUNTIME_ERROR = 4;
    public static final int SYSTEM_ERROR = 5;
    public static final int ACCEPTED = 1;

    // 配置文件结构
    public static class Config {
        public int timeLimit;
        public JsonNode checkpoints;
    }

    public static String JServer(String jsonConfig, File cppFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 解析JSON文本
            Config config = mapper.readValue(jsonConfig, Config.class);

            // 获取检查点数量
            JsonNode checkpoints = config.checkpoints;
            int checkpointsCount = checkpoints.size() / 2;

            // 线程池
            ExecutorService executor = Executors.newFixedThreadPool(checkpointsCount);
            List<Future<Judger.JudgeResult>> futures = new ArrayList<>();

            // 动态生成检查点任务
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

            // 收集所有线程的结果
            List<Judger.JudgeResult> results = new ArrayList<>();
            for (Future<Judger.JudgeResult> future : futures) {
                try {
                    Judger.JudgeResult result = future.get();
                    results.add(result);
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error(e.getMessage());
                    results.add(new Judger.JudgeResult(SYSTEM_ERROR, 0.0)); // 如果出现异常，默认返回 SYSTEM_ERROR
                }
            }

            // 输出结果
            LOGGER.info("========== Results ==========");
            for (int i = 0; i < results.size(); i++) {
                Judger.JudgeResult result = results.get(i);
                String status = getStatusDescription(result.statusCode);
                LOGGER.info("Checkpoint {} result: {} ({}), Time: {}ms", i + 1, result.statusCode, status, result.timeMs);
            }

            // 构建判题结果
            StringBuilder jsonResult = new StringBuilder();
            jsonResult.append("{");
            for (int i = 0; i < results.size(); i++) {
                Judger.JudgeResult result = results.get(i);
                if (i > 0) {
                    jsonResult.append(",");
                }
                jsonResult.append("\"").append(i + 1).append("_res\":").append(result.statusCode)
                        .append(",\"").append(i + 1).append("_time\":").append(result.timeMs);
            }
            jsonResult.append("}");
            return jsonResult.toString();

        } catch (Exception e) {
            LOGGER.error("Failed to execute judge tasks: {}", e.getMessage());
            return "{}";
        }
    }

    private static String getStatusDescription(int statusCode) {
        return switch (statusCode) {
            case COMPILE_ERROR -> "Compile Error";
            case WRONG_ANSWER -> "Wrong Answer";
            case REAL_TIME_LIMIT_EXCEEDED -> "Real Time Limit Exceeded";
            case RUNTIME_ERROR -> "Runtime Error";
            case SYSTEM_ERROR -> "System Error";
            case ACCEPTED -> "Accepted";
            default -> "Unknown Status";
        };
    }
}