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
        public final double timeMs;

        public JudgeResult(int statusCode, double timeMs) {
            this.statusCode = statusCode;
            this.timeMs = timeMs;
        }
    }

    // 判题机
    public static JudgeResult judge(File programPath, File inFile, File outFile, int timeMs) {
        Random random = new Random();
        String programName = "c_" + random.nextInt(1000000);
        LOGGER.info("Compiling program: {}", programName);

        // 如果是 Windows 系统，添加 .exe 后缀
        if (isWindows()) {
            programName += ".exe";
        }

        File executableFile = new File(programName);

        try {
            // 编译
            if (compileProgram(programPath, executableFile) != 0) {
                return new JudgeResult(COMPILE_ERROR, 0.0); // 编译失败
            }

            // 运行程序
            Process runProcess;
            double elapsedTimeMs = 0.0;
            try {
                RunResult runResult = runProgram(executableFile, inFile, timeMs);
                runProcess = runResult.process;
                elapsedTimeMs = runResult.elapsedTimeMs;
            } catch (TimeoutException e) {
                // 超时情况下，仍然返回实际的运行时间
                return new JudgeResult(REAL_TIME_LIMIT_EXCEEDED, elapsedTimeMs); // 超时
            } catch (IOException | InterruptedException e) {
                return new JudgeResult(SYSTEM_ERROR, 0.0); // 系统错误
            }

            // 检查运行结果
            if (runProcess.exitValue() != 0) {
                return new JudgeResult(RUNTIME_ERROR, elapsedTimeMs); // 运行时错误
            }

            // 验证输出
            if (!compareOutput(runProcess.getInputStream(), outFile)) {
                return new JudgeResult(WRONG_ANSWER, elapsedTimeMs); // 答案错误
            }

            return new JudgeResult(ACCEPTED, elapsedTimeMs); // 答案正确

        } catch (IOException | InterruptedException e) {
            LOGGER.error("IO error occurred: {}", e.getMessage());
            return new JudgeResult(SYSTEM_ERROR, 0.0); // 系统错误
        } finally {
            // 无论结果如何，最后都删除编译文件
            if (executableFile.exists()) {
                boolean isDeleted = executableFile.delete();
                if (isDeleted) {
                    LOGGER.debug("Deleted executable file: {}", executableFile.getAbsolutePath());
                } else {
                    LOGGER.error("Failed to delete the executable file: {}", executableFile.getAbsolutePath());
                }
            }
        }
    }

    // 判断是否是 Windows 系统
    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    // 编译程序
    private static int compileProgram(File programPath, File executableFile) throws IOException, InterruptedException {
        ProcessBuilder compileBuilder = new ProcessBuilder("g++", "-o", executableFile.getName(), programPath.getAbsolutePath(), "-std=c++11");
        compileBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
        Process compileProcess = compileBuilder.start();

        // 捕获编译输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
            }
        }
        return compileProcess.waitFor();
    }

    // 封装运行结果
    private static class RunResult {
        public final Process process;
        public final double elapsedTimeMs;

        public RunResult(Process process, double elapsedTimeMs) {
            this.process = process;
            this.elapsedTimeMs = elapsedTimeMs;
        }
    }

    // 运行程序
    private static RunResult runProgram(File executableFile, File inFile, int timeMs) throws IOException, InterruptedException, TimeoutException {
        String command = isWindows() ? executableFile.getName() : "./" + executableFile.getName();
        ProcessBuilder runBuilder = new ProcessBuilder(command);
        runBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
        Process runProcess = runBuilder.start();
        long startTime = System.nanoTime();

        // 输入重定向
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()));
             BufferedReader inputReader = new BufferedReader(new FileReader(inFile))) {
            String line;
            while ((line = inputReader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }

        // 设置超时
        if (!runProcess.waitFor(timeMs, TimeUnit.MILLISECONDS)) {
            long endTime = System.nanoTime();
            double elapsedTimeMs = (endTime - startTime) / 1_000_000.0;
            runProcess.destroyForcibly(); // 强制终止进程

            // 确保进程资源被释放
            try {
                LOGGER.debug("Waiting for process {} resources to be released...",runProcess.exitValue());
                Thread.sleep(10); // 100ms 延迟
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for process to terminate: {}", e.getMessage());
            }

            throw new TimeoutException("Process timed out after " + elapsedTimeMs + " ms");
        }

        long endTime = System.nanoTime();
        double elapsedTimeMs = (endTime - startTime) / 1_000_000.0;

        return new RunResult(runProcess, elapsedTimeMs);
    }

    // 比较输出
    private static boolean compareOutput(InputStream actualOutput, File expectedOutputFile) throws IOException {
        try (BufferedReader expectedReader = new BufferedReader(new FileReader(expectedOutputFile));
             BufferedReader actualReader = new BufferedReader(new InputStreamReader(actualOutput))) {
            String expectedLine, actualLine;
            while ((expectedLine = expectedReader.readLine()) != null) {
                actualLine = actualReader.readLine();
                if (!expectedLine.equals(actualLine)) {
                    return false; // 输出不匹配
                }
            }
            return actualReader.readLine() == null;
        }
    }
}