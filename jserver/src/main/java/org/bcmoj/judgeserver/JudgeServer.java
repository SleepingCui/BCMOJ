package org.bcmoj.judgeserver;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.config.JudgeTaskConfig;
import org.bcmoj.judger.Compiler;
import org.bcmoj.judger.Judger;
import org.bcmoj.security.RegexSecurityCheck;
import org.bcmoj.security.SecurityChecker;
import org.bcmoj.utils.JudgeResultUtil;
import org.bcmoj.utils.OutputCompareUtil;
import org.bcmoj.utils.FileUtil;
import org.bcmoj.utils.JsonReadUtil;
import org.bcmoj.utils.JsonValidateUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * JudgeServer is responsible for handling the judging process of submitted C++ programs
 * against multiple test case checkpoints.
 *
 * <p>Status codes returned by {@link Judger} and used in results:</p>
 * <ul>
 *     <li>-5: Security Check Failed</li>
 *     <li>-4: Compile Error</li>
 *     <li>-3: Wrong Answer</li>
 *     <li>2: Real Time Limit Exceeded</li>
 *     <li>3: Memory Limit Exceeded</li>
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
 * <p>This implementation compiles the source code only once and runs multiple threads for
 * each checkpoint, which reduces compilation overhead and resource usage.</p>
 *
 * @author SleepingCui
 */
@Slf4j
public class JudgeServer {

    /**
     * Serves judging requests for a C++ program.
     * This version accepts a JudgeTaskConfig object containing all necessary parameters.
     *
     * @param taskConfig       The configuration object containing compiler, file paths, flags, etc.
     * @param jsonConfig       JSON string containing checkpoints, time limits, mem limits, and flags
     * @return JSON string representing aggregated judge results
     */
    public static String serve(JudgeTaskConfig taskConfig, String jsonConfig) { // New signature
        File tempDir = null;
        File exeFile = null;
        ExecutorService executor = null;
        try {
            JsonReadUtil.ConfigResult configResult = JsonReadUtil.parseConfig(jsonConfig, taskConfig.isUseOldFormat());

            boolean securityCheckFailed;
            if (configResult.securityCheck) {
                SecurityChecker checker = new RegexSecurityCheck();
                int securityCheckResult = checker.check(taskConfig.getSourceFile(), taskConfig.getKeywordFile());
                securityCheckFailed = (securityCheckResult == -5);
                if (securityCheckFailed) {
                    log.warn("Security check failed for file: {}", taskConfig.getSourceFile().getName());
                    int count = configResult.useOldFormat ? new JsonValidateUtil().countIns(configResult.checkpoints, true) : new JsonValidateUtil().countIns(configResult.checkpoints, false);
                    return JudgeResultUtil.buildResult(null, true, false, count, taskConfig.isUseOldFormat());
                }
            } else {
                log.info("Code Security Check is not enabled");
            }

            tempDir = Files.createTempDirectory("judge_").toFile();
            String exeName = java.util.UUID.randomUUID().toString().replace("-", "");
            if (System.getProperty("os.name").toLowerCase().contains("win")) exeName += ".exe";
            exeFile = new File(tempDir, exeName);

            log.info("Compiling file: {} with enableO2={} , disableSecurityArgs={}", taskConfig.getSourceFile().getAbsolutePath(), configResult.enableO2, taskConfig.isDisableSecurityArgs());
            int compileCode = Compiler.compileProgram(taskConfig.getSourceFile(), exeFile, configResult.enableO2, taskConfig.isDisableSecurityArgs(), 10_000, taskConfig.getCompilerPath(), taskConfig.getCppStandard());
            if (compileCode != 0) {
                List<Judger.JudgeResult> compileFailResults = new ArrayList<>();
                for (int i = 0; i < configResult.checkpointsCount; i++) {
                    compileFailResults.add(new Judger.JudgeResult(-4, 0.0, 0L));
                }
                return JudgeResultUtil.buildResult(compileFailResults, false, false, configResult.checkpointsCount, taskConfig.isUseOldFormat());
            }

            OutputCompareUtil.CompareMode mode = switch (configResult.compareMode) {
                case 2 -> OutputCompareUtil.CompareMode.IGNORE_SPACES;
                case 3 -> OutputCompareUtil.CompareMode.CASE_INSENSITIVE;
                case 4 -> OutputCompareUtil.CompareMode.FLOAT_TOLERANT;
                default -> OutputCompareUtil.CompareMode.STRICT;
            };

            executor = Executors.newFixedThreadPool(configResult.checkpointsCount);
            List<Future<Judger.JudgeResult>> futures = new ArrayList<>();

            if (configResult.checkpointsCount > 0) {
                for (int i = 0; i < configResult.checkpointsCount; i++) {
                    final String input;
                    final String output;

                    if (configResult.useOldFormat) {
                        input = configResult.checkpoints.get((i + 1) + "_in").asText();
                        output = configResult.checkpoints.get((i + 1) + "_out").asText();
                    } else {
                        String key = String.valueOf(i + 1);
                        JsonNode checkpoint = configResult.checkpoints.get(key);
                        input = checkpoint.get("in").asText();
                        output = checkpoint.get("out").asText();
                    }
                    File finalExeFile = exeFile;
                    Future<Judger.JudgeResult> future = executor.submit(() ->
                            Judger.judge(finalExeFile, input, output, configResult.timeLimit, configResult.memLimit, mode, taskConfig.isDisableMemLimit())
                    );
                    futures.add(future);
                }
            }
            executor.shutdown();

            List<Judger.JudgeResult> results = new ArrayList<>();
            for (Future<Judger.JudgeResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Checkpoint execution error: {}", e.getMessage(), e);
                    results.add(new Judger.JudgeResult(5, 0.0, 0L));
                }
            }
            log.info("========== Results ==========");
            for (int i = 0; i < results.size(); i++) {
                Judger.JudgeResult result = results.get(i);
                log.info("Checkpoint {} result: {} ({}), Time: {}ms, Memory: {}KB", i + 1, result.statusCode, StatusDescription(result.statusCode), result.time, result.maxMemoryUsedKB);
            }
            FileUtil.deleteRecursively(exeFile);
            FileUtil.deleteRecursively(tempDir);

            return JudgeResultUtil.buildResult(results, false, false, configResult.checkpointsCount, taskConfig.isUseOldFormat()); // Use format from config

        } catch (Exception e) {
            log.error("Failed to execute judge tasks: {}", e.getMessage(), e);
            return JudgeResultUtil.buildResult(null, false, true, 1, taskConfig.isUseOldFormat()); // Use format from config
        }
        finally {
            if (executor != null && !executor.isShutdown()) { executor.shutdownNow();}
            if (exeFile != null) { FileUtil.deleteRecursively(exeFile); }
            if (tempDir != null) { FileUtil.deleteRecursively(tempDir); }
        }
    }

    private static String StatusDescription(int statusCode) {
        return switch (statusCode) {
            case -5 -> "Security Check Failed";
            case -4 -> "Compile Error";
            case -3 -> "Wrong Answer";
            case 2 -> "Real Time Limit Exceeded";
            case 3 -> "Memory Limit Exceeded";
            case 4 -> "Runtime Error";
            case 5 -> "System Error";
            case 1 -> "Accepted";
            default -> "Unknown Status";
        };
    }
}