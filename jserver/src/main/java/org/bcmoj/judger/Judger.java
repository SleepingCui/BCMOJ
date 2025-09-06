package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.OutputCompareUtil;
import org.bcmoj.utils.StringUtil;

import java.io.File;
import java.nio.file.Files;

/**
 * Judger provides methods to evaluate a compiled C++ executable against test cases.
 *
 * <p>This class defines standard status codes and the {@link JudgeResult} class for
 * reporting the outcome of each test case.</p>
 *
 * <p>It supports multi-threaded judging by allowing each thread to run a copy of the
 * original executable to avoid conflicts.</p>
 *
 * Status codes:
 * <ul>
 *     <li>-4: Compile Error</li>
 *     <li>-3: Wrong Answer</li>
 *     <li>2: Real Time Limit Exceeded</li>
 *     <li>4: Runtime Error</li>
 *     <li>5: System Error</li>
 *     <li>1: Accepted</li>
 * </ul>
 *
 * Comparison modes are defined in {@link OutputCompareUtil.CompareMode}.
 * @author SleepingCui
 */
@Slf4j
public class Judger {

    public static final int COMPILE_ERROR = -4;
    public static final int WRONG_ANSWER = -3;
    public static final int REAL_TIME_LIMIT_EXCEEDED = 2;
    public static final int RUNTIME_ERROR = 4;
    public static final int SYSTEM_ERROR = 5;
    public static final int ACCEPTED = 1;

    public static class JudgeResult {
        public final int statusCode;
        public final double time;

        public JudgeResult(int statusCode, double time) {
            this.statusCode = statusCode;
            this.time = time;
            log.debug("Judge process finished. Status code: {}, Elapsed time: {} ms", statusCode, time);
        }
    }


    /**
     * Judges a compiled C++ executable against a single test case.
     *
     * @param originalExe The compiled executable file
     * @param inputContent The input string for the test case
     * @param expectedOutputContent The expected output string
     * @param time Time limit in milliseconds
     * @param compareMode Output comparison mode
     * @return {@link JudgeResult} containing status code and execution time
     */
    public static JudgeResult judge(File originalExe, String inputContent, String expectedOutputContent, int time, OutputCompareUtil.CompareMode compareMode) {
        File tempExe = null;
        try {
            tempExe = Files.createTempFile("exe_copy_", System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "").toFile();
            Files.copy(originalExe.toPath(), tempExe.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Runner.RunResult runResult = Runner.runProgram(tempExe, StringUtil.unescapeString(inputContent), time);

            if (runResult.exitCode != 0) {
                log.warn("Runtime error, exit code {}", runResult.exitCode);
                return new JudgeResult(RUNTIME_ERROR, runResult.elapsedTime);
            }

            boolean outputMatches = OutputCompareUtil.compare(runResult.output, StringUtil.unescapeString(expectedOutputContent), compareMode);
            return outputMatches ? new JudgeResult(ACCEPTED, runResult.elapsedTime) : new JudgeResult(WRONG_ANSWER, runResult.elapsedTime);

        } catch (TimeoutException e) {
            log.warn("Execution timed out after {} ms", e.getElapsedTime());
            return new JudgeResult(REAL_TIME_LIMIT_EXCEEDED, e.getElapsedTime());
        } catch (Exception e) {
            log.error("System error: {}", e.getMessage(), e);
            return new JudgeResult(SYSTEM_ERROR, 0.0);
        } finally {
            if (tempExe != null && tempExe.exists()) {
                if (!tempExe.delete()) {
                    log.warn("Failed to delete temp exe copy: {}", tempExe.getAbsolutePath());
                }
            }
        }
    }
}
