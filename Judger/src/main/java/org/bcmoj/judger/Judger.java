package org.bcmoj.judger;

import java.io.*;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BCMOJ Judger Component
 * <p>
 * Core judging component responsible for compiling, executing, and evaluating submitted programs.
 * Handles the complete judging workflow including compilation, runtime execution, output comparison,
 * and resource management.
 * </p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Supports multiple verdict states (AC, WA, TLE, RTE, etc.)</li>
 *   <li>Precise time measurement for program execution</li>
 *   <li>Cross-platform support (Windows/Linux)</li>
 *   <li>Secure execution with proper resource cleanup</li>
 *   <li>Escape sequence handling for test cases</li>
 * </ul>
 *
 * @author SleepingCui
 * @version 1.0-SNAPSHOT
 * @since 2025
 * @see <a href="https://github.com/SleepingCui/bcmoj-judge-server">Github Repository</a>
 */
public class Judger {

    public static Logger LOGGER = LoggerFactory.getLogger(Judger.class);
    // 状态码
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
        }
    }

    public static JudgeResult judge(File programPath, String inputContent, String expectedOutputContent, int time) {
        Random random = new Random();
        String programName = "c_" + random.nextInt(1000000);
        LOGGER.info("Compiling program: {}", programName);

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            programName += ".exe";
        }
        File executableFile = new File(programName);
        try {
            if (compileProgram(programPath, executableFile) != 0) {
                return new JudgeResult(COMPILE_ERROR, 0.0);
            }
            String processedInput = unescapeString(inputContent);

            Process runProcess;
            double elapsedTime = 0.0;
            try {
                RunResult runResult = runProgram(executableFile, processedInput, time);
                runProcess = runResult.process;
                elapsedTime = runResult.elapsedTime;
            } catch (TimeoutException e) {
                return new JudgeResult(REAL_TIME_LIMIT_EXCEEDED, elapsedTime);
            } catch (IOException | InterruptedException e) {
                return new JudgeResult(SYSTEM_ERROR, 0.0);
            }
            if (runProcess.exitValue() != 0) {
                return new JudgeResult(RUNTIME_ERROR, elapsedTime);
            }
            String processedExpected = unescapeString(expectedOutputContent);
            if (!compareOutput(runProcess.getInputStream(), processedExpected)) {
                return new JudgeResult(WRONG_ANSWER, elapsedTime);
            }
            return new JudgeResult(ACCEPTED, elapsedTime);

        } catch (IOException | InterruptedException e) {
            LOGGER.error("IO error occurred: {}", e.getMessage());
            return new JudgeResult(SYSTEM_ERROR, 0.0);
        } finally {
            if (executableFile.exists()) {
                boolean isDeleted = executableFile.delete();
                if (!isDeleted) {
                    LOGGER.warn("Failed to delete the executable file: {}", executableFile.getAbsolutePath());
                }
            }
        }
    }

    private static int compileProgram(File programPath, File executableFile) throws IOException, InterruptedException {
        ProcessBuilder compileBuilder = new ProcessBuilder("g++", "-o", executableFile.getName(), programPath.getAbsolutePath(), "-std=c++11");
        compileBuilder.redirectErrorStream(true);
        Process compileProcess = compileBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info("[Compiler] {}",line);
            }
        }
        return compileProcess.waitFor();
    }

    private static class RunResult {
        public final Process process;
        public final double elapsedTime;

        public RunResult(Process process, double elapsedTime) {
            this.process = process;
            this.elapsedTime = elapsedTime;
        }
    }

    private static RunResult runProgram(File executableFile, String inputContent, int time)
            throws IOException, InterruptedException, TimeoutException {
        String command = System.getProperty("os.name").toLowerCase().contains("win")
                ? executableFile.getName()
                : "./" + executableFile.getName();
        ProcessBuilder runBuilder = new ProcessBuilder(command);
        runBuilder.redirectErrorStream(true);
        Process runProcess = runBuilder.start();

        long startTime = System.nanoTime();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()))) {
            writer.write(inputContent);
            writer.flush();
        }

        if (!runProcess.waitFor(time, TimeUnit.MILLISECONDS)) {
            long endTime = System.nanoTime();
            double elapsedTime = (endTime - startTime) / 1_000_000.0;
            runProcess.destroyForcibly();
            try {
                LOGGER.debug("Waiting for process {} resources to be released...", runProcess.exitValue());
                Thread.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for process to terminate: {}", e.getMessage());
            }
            throw new TimeoutException("Process timed out after " + elapsedTime + " ms");
        }

        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1_000_000.0;
        return new RunResult(runProcess, elapsedTime);
    }

    private static boolean compareOutput(InputStream actualOutput, String expectedOutputContent) throws IOException {
        try (BufferedReader actualReader = new BufferedReader(new InputStreamReader(actualOutput))) {
            StringBuilder actualOutputContent = new StringBuilder();
            String line;
            while ((line = actualReader.readLine()) != null) {
                actualOutputContent.append(line).append("\n");
            }
            if (!actualOutputContent.isEmpty()) {
                actualOutputContent.setLength(actualOutputContent.length() - 1);
            }
            return expectedOutputContent.contentEquals(actualOutputContent);
        }
    }

    private static final Map<Character, Character> ESCAPE_MAP = Map.of(
            'n', '\n',
            't', '\t',
            'r', '\r',
            '\\', '\\',
            '\"', '\"',
            '\'', '\''
    );

    private static String unescapeString(String str) {
        if (str == null) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                if (ESCAPE_MAP.containsKey(next)) {
                    sb.append(ESCAPE_MAP.get(next));
                    i++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}