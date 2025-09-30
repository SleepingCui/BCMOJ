package org.bcmoj.judgeserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Compiler;
import org.bcmoj.judger.Judger;
import org.bcmoj.security.RegexSecurityCheck;
import org.bcmoj.security.SecurityChecker;
import org.bcmoj.utils.JudgeResultUtil;
import org.bcmoj.utils.OutputCompareUtil;
import org.bcmoj.utils.FileUtil;

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
     * @param jsonConfig       JSON string containing checkpoints, time limits, and flags
     * @param cppFilePath      path to the submitted C++ source file
     * @param keywordsFilePath path to the keyword file used for security check
     * @param compilerPath     path to the compiler
     * @param cppStandard      C++ standard version
     * @param DisableSecurityArgs Disable Compiler security flags
     * @return JSON string representing aggregated judge results
     */
    public static String serve(String jsonConfig, String compilerPath, String cppStandard, File cppFilePath, File keywordsFilePath, boolean DisableSecurityArgs) {
        ObjectMapper mapper = new ObjectMapper();
        File tempDir = null;
        File exeFile = null;
        ExecutorService executor = null;
        try {
            Config config = mapper.readValue(jsonConfig, Config.class);
            JsonNode checkpoints = config.checkpoints;
            int checkpointsCount = checkpoints.size() / 2;

            boolean securityCheckFailed;
            if (config.securityCheck) {
                SecurityChecker checker = new RegexSecurityCheck();
                int securityCheckResult = checker.check(cppFilePath, keywordsFilePath);
                securityCheckFailed = (securityCheckResult == -5);
                if (securityCheckFailed) {
                    log.warn("Security check failed for file: {}", cppFilePath.getName());
                    return JudgeResultUtil.buildResult(null, true, false, checkpointsCount);
                }
            } else {
                log.info("Code Security Check is not enabled");
            }

            tempDir = Files.createTempDirectory("judge_").toFile();
            String exeName = java.util.UUID.randomUUID().toString().replace("-", "");
            if (System.getProperty("os.name").toLowerCase().contains("win")) exeName += ".exe";
            exeFile = new File(tempDir, exeName);

            log.info("Compiling file: {} with enableO2={} , disableSecurityArgs={}", cppFilePath.getAbsolutePath(), config.enableO2, DisableSecurityArgs);
            int compileCode = Compiler.compileProgram(cppFilePath, exeFile, config.enableO2, DisableSecurityArgs, 10_000, compilerPath, cppStandard);
            if (compileCode != 0) {
                List<Judger.JudgeResult> compileFailResults = new ArrayList<>();
                for (int i = 0; i < checkpointsCount; i++) {
                    compileFailResults.add(new Judger.JudgeResult(-4, 0.0));
                }
                return JudgeResultUtil.buildResult(compileFailResults, false, false, checkpointsCount);
            }

            OutputCompareUtil.CompareMode mode = switch (config.compareMode) {
                case 2 -> OutputCompareUtil.CompareMode.IGNORE_SPACES;
                case 3 -> OutputCompareUtil.CompareMode.CASE_INSENSITIVE;
                case 4 -> OutputCompareUtil.CompareMode.FLOAT_TOLERANT;
                default -> OutputCompareUtil.CompareMode.STRICT;
            };

            executor = Executors.newFixedThreadPool(checkpointsCount);
            List<Future<Judger.JudgeResult>> futures = new ArrayList<>();
            for (int i = 1; i <= checkpointsCount; i++) {
                final String input = checkpoints.get(i + "_in").asText();
                final String output = checkpoints.get(i + "_out").asText();
                File finalExeFile = exeFile;
                Future<Judger.JudgeResult> future = executor.submit(() ->
                        Judger.judge(finalExeFile, input, output, config.timeLimit, mode)
                );
                futures.add(future);
            }
            executor.shutdown();

            List<Judger.JudgeResult> results = new ArrayList<>();
            for (Future<Judger.JudgeResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Checkpoint execution error: {}", e.getMessage(), e);
                    results.add(new Judger.JudgeResult(5, 0.0));
                }
            }
            log.info("========== Results ==========");
            for (int i = 0; i < results.size(); i++) {
                Judger.JudgeResult result = results.get(i);
                log.info("Checkpoint {} result: {} ({}), Time: {}ms", i + 1, result.statusCode, StatusDescription(result.statusCode), result.time);
            }
            FileUtil.deleteRecursively(exeFile);
            FileUtil.deleteRecursively(tempDir);

            return JudgeResultUtil.buildResult(results, false, false, checkpointsCount);

        } catch (Exception e) {
            log.error("Failed to execute judge tasks: {}", e.getMessage(), e);
            return JudgeResultUtil.buildResult(null, false, true, 1);
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
            case 4 -> "Runtime Error";
            case 5 -> "System Error";
            case 1 -> "Accepted";
            default -> "Unknown Status";
        };
    }
}
