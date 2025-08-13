package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.OutputCompareUtil;
import org.bcmoj.utils.StringUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Core judging engine for C++ programs.
 *
 * <p>This class handles the entire judging process including:
 * <ul>
 *   <li>Compiling the submitted program</li>
 *   <li>Running the executable with provided input</li>
 *   <li>Comparing the output with the expected output</li>
 *   <li>Returning a status code based on the result</li>
 * </ul>
 *
 * <p>Temporary directories and files are automatically cleaned up
 * after judging is complete.</p>
 *
 * <p>Logging is provided for compilation, execution, and cleanup stages.</p>
 *
 * <p>Status codes:</p>
 * <ul>
 *   <li>COMPILE_ERROR = -4</li>
 *   <li>WRONG_ANSWER = -3</li>
 *   <li>REAL_TIME_LIMIT_EXCEEDED = 2</li>
 *   <li>RUNTIME_ERROR = 4</li>
 *   <li>SYSTEM_ERROR = 5</li>
 *   <li>ACCEPTED = 1</li>
 * </ul>
 *
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
     * Judges a submitted C++ program against a test case.
     *
     * @param programPath           path to the source code file
     * @param inputContent          test input string
     * @param expectedOutputContent expected output string
     * @param time                  time limit in milliseconds
     * @param enableO2              whether to enable O2 optimization
     * @param compareMode           output comparison mode
     * @return JudgeResult containing status code and execution time
     *
     * <p>Output comparison modes (OutputCompareUtil.CompareMode):</p>
     * <ul>
     * <li>STRICT: strict character-by-character comparison</li>
     * <li>IGNORE_SPACES: ignores spaces and tabs</li>
     * <li>CASE_INSENSITIVE: ignores letter case</li>
     * <li>FLOAT_TOLERANT: allows small floating-point deviations</li>
     * </ul>
     */

    public static JudgeResult judge(File programPath, String inputContent, String expectedOutputContent, int time, boolean enableO2, OutputCompareUtil.CompareMode compareMode) {
        File executableFile = null;
        try {
            Path tempDir = Files.createTempDirectory("judge_");
            String exeName = UUID.randomUUID().toString().replace("-", "");
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                exeName += ".exe";
            }
            executableFile = new File(tempDir.toFile(), exeName);

            log.info("Compiling program: {} with O2 optimization: {}", executableFile.getName(), enableO2);
            int compileCode = Compiler.compileProgram(programPath, executableFile, enableO2, 10_000);
            if (compileCode != 0) {
                return new JudgeResult(COMPILE_ERROR, 0.0);
            }

            Runner.RunResult runResult = Runner.runProgram(executableFile, StringUtil.unescapeString(inputContent), time);
            if (runResult.exitCode != 0) {
                log.warn("Runtime error, exit code {}", runResult.exitCode);
                return new JudgeResult(RUNTIME_ERROR, runResult.elapsedTime);
            }
            boolean outputMatches = OutputCompareUtil.compare(runResult.output, StringUtil.unescapeString(expectedOutputContent), compareMode);
            return outputMatches ? new JudgeResult(ACCEPTED, runResult.elapsedTime) : new JudgeResult(WRONG_ANSWER, runResult.elapsedTime);
        } catch (Runner.TimeoutException e) {
            log.warn("Execution timed out after {} ms", e.getElapsedTime());
            return new JudgeResult(REAL_TIME_LIMIT_EXCEEDED, e.getElapsedTime());
        } catch (Exception e) {
            log.error("System error: {}", e.getMessage(), e);
            return new JudgeResult(SYSTEM_ERROR, 0.0);
        } finally {
            try {
                if (executableFile != null) {
                    File parent = executableFile.getParentFile();
                    if (parent != null && parent.getName().startsWith("judge_")) {
                        deleteRecursively(parent);
                    } else {
                        deleteRecursively(executableFile);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to clean up temp files: {}", ex.getMessage(), ex);
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            log.warn("Failed to delete: {}", file.getAbsolutePath());
        } else {
            log.debug("Deleted: {}", file.getAbsolutePath());
        }
    }

}
