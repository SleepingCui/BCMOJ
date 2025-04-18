package org.bcmoj.judger;

import java.io.*;
import java.util.Random;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Judger {

    public static Logger LOGGER = LoggerFactory.getLogger(Judger.class);
    // 状态码
    public static final int COMPILE_ERROR = -4;
    public static final int WRONG_ANSWER = -3;
    public static final int REAL_TIME_LIMIT_EXCEEDED = 2;
    public static final int RUNTIME_ERROR = 4;
    public static final int SYSTEM_ERROR = 5;
    public static final int ACCEPTED = 1;

    // 封装状态码和运行时间的类
    public static class JudgeResult {
        public final int statusCode;
        public final double time;

        public JudgeResult(int statusCode, double time) {
            this.statusCode = statusCode;
            this.time = time;
        }
    }
    // 判题机
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
            // 运行程序
            Process runProcess;
            double elapsedTime = 0.0;
            try {
                RunResult runResult = runProgram(executableFile, inputContent, time);
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
            if (!compareOutput(runProcess.getInputStream(), expectedOutputContent)) {
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
    // 编译程序
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
    // 运行程序
    private static RunResult runProgram(File executableFile, String inputContent, int time) throws IOException, InterruptedException, TimeoutException {
        String command = System.getProperty("os.name").toLowerCase().contains("win") ? executableFile.getName() : "./" + executableFile.getName();
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
    // 比较输出
    private static boolean compareOutput(InputStream actualOutput, String expectedOutputContent) throws IOException {
        try (BufferedReader actualReader = new BufferedReader(new InputStreamReader(actualOutput))) {
            StringBuilder actualOutputContent = new StringBuilder();
            String line;
            while ((line = actualReader.readLine()) != null) {
                actualOutputContent.append(line).append("\n");
            }
            if (actualOutputContent.length() > 0) {
                actualOutputContent.setLength(actualOutputContent.length() - 1);
            }
            return expectedOutputContent.contentEquals(actualOutputContent);
        }
    }
}