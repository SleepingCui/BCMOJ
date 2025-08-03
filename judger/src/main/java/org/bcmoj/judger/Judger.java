package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

@Slf4j
public class Judger {
    public static final int COMPILE_ERROR = -4;
    public static final int WRONG_ANSWER = -3;
    public static final int REAL_TIME_LIMIT_EXCEEDED = 2;
    public static final int RUNTIME_ERROR = 4;
    public static final int SYSTEM_ERROR = 5;
    public static final int ACCEPTED = 1;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static class JudgeResult {
        public final int statusCode;
        public final double time;

        public JudgeResult(int statusCode, double time) {
            this.statusCode = statusCode;
            this.time = time;
        }
    }

    private static class RunResult {
        public final Process process;
        public final double elapsedTime;

        public RunResult(Process process, double elapsedTime) {
            this.process = process;
            this.elapsedTime = elapsedTime;
        }
    }

    public static JudgeResult judge(File programPath, String inputContent, String expectedOutputContent, int time, boolean enableO2) {
        Random random = new Random();
        String programName = "c_" + random.nextInt(1000000);
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            programName += ".exe";
        }
        File executableFile = new File(programName);
        log.info("Compiling program: {} with O2 optimization: {}", programName, enableO2);

        Future<Integer> compileTask = executor.submit(() -> compileProgram(programPath, executableFile, enableO2));
        try {
            int compileCode = compileTask.get(10, TimeUnit.SECONDS);
            if (compileCode != 0) return new JudgeResult(COMPILE_ERROR, 0.0);
        } catch (Exception e) {
            log.error("Compilation failed: {}", e.getMessage());
            return new JudgeResult(COMPILE_ERROR, 0.0);
        }
        try {
            String processedInput = unescapeString(inputContent);
            RunResult runResult = runProgram(executableFile, processedInput, time);
            Process runProcess = runResult.process;

            double elapsedTime = runResult.elapsedTime;
            int exitCode = runProcess.isAlive() ? runProcess.waitFor() : runProcess.exitValue();
            if (exitCode != 0) return new JudgeResult(RUNTIME_ERROR, elapsedTime);
            String expectedOutput = unescapeString(expectedOutputContent);
            Future<Boolean> compareTask = executor.submit(() -> compareOutput(runProcess.getInputStream(), expectedOutput));
            boolean outputMatches = compareTask.get(3, TimeUnit.SECONDS);
            return outputMatches ? new JudgeResult(ACCEPTED, elapsedTime) : new JudgeResult(WRONG_ANSWER, elapsedTime);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("TIMEOUT:")) {
                double actualTime = Double.parseDouble(e.getMessage().substring(8));
                return new JudgeResult(REAL_TIME_LIMIT_EXCEEDED, actualTime);
            }
            throw e;
        } catch (TimeoutException e) {
            return new JudgeResult(REAL_TIME_LIMIT_EXCEEDED, time);
        } catch (Exception e) {
            log.error("System error: {}", e.getMessage());
            return new JudgeResult(SYSTEM_ERROR, 0.0);
        } finally {
            executor.submit(() -> {
                if (executableFile.exists() && !executableFile.delete()) {
                    log.warn("Failed to delete executable: {}", executableFile.getAbsolutePath());
                }
            });
        }
    }
    private static int compileProgram(File programPath, File executableFile, boolean enableO2) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("g++");
        command.add("-o");
        command.add(executableFile.getName());
        command.add(programPath.getAbsolutePath());
        command.add("-std=c++11");
        if (enableO2) command.add("-O2");
        log.debug("Command: {}", command);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> log.info("[Compiler] {}", line));
        }
        return process.waitFor();
    }

    private static RunResult runProgram(File executableFile, String inputContent, int time) throws IOException, InterruptedException {
        String command = System.getProperty("os.name").toLowerCase().contains("win") ? executableFile.getName() : "./" + executableFile.getName();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        long startTime = System.nanoTime();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(inputContent);
            writer.flush();
        }
        boolean finished = process.waitFor(time, TimeUnit.MILLISECONDS);
        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1_000_000.0;
        if (!finished) {
            process.destroyForcibly();
            try {
                process.waitFor();
                Thread.sleep(10);
            } catch (InterruptedException e) {
                log.warn("Interrupted during force kill: {}", e.getMessage());
            }
            throw new RuntimeException("TIMEOUT:" + elapsedTime);
        }
        return new RunResult(process, elapsedTime);
    }

    private static boolean compareOutput(InputStream actualOutput, String expectedOutputContent) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(actualOutput))) {
            StringBuilder actual = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                actual.append(line).append("\n");
            }
            if (!actual.isEmpty()) actual.setLength(actual.length() - 1);
            return expectedOutputContent.contentEquals(actual);
        }
    }

    private static final Map<Character, Character> ESCAPE_MAP = Map.of(
            'n', '\n', 't', '\t', 'r', '\r',
            '\\', '\\', '\"', '\"', '\'', '\''
    );

    private static String unescapeString(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length() && ESCAPE_MAP.containsKey(str.charAt(i + 1))) {
                sb.append(ESCAPE_MAP.get(str.charAt(++i)));
            } else sb.append(c);
        }
        return sb.toString();
    }
}
