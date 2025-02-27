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

    // 判题机
    public static int judge(File programPath, File inFile, File outFile, int timeMs) {

        Random random = new Random();
        String programName = "c_" + random.nextInt(1000000);
        LOGGER.info("compiling program: " + programName);

        // 如果是 Windows 系统，添加 .exe 后缀
        if (isWindows()) {
            programName += ".exe";
        }

        File executableFile = new File(programName);

        // 编译
        try {
            if (compileProgram(programPath, executableFile) != 0) {
                LOGGER.info("Result: COMPILE_ERROR", COMPILE_ERROR);
                return COMPILE_ERROR; // 编译失败
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Result: SYSTEM_ERROR",SYSTEM_ERROR,"\n",e.getMessage());
            return SYSTEM_ERROR; // 系统错误
        }

        // 运行程序
        Process runProcess;
        try {
            runProcess = runProgram(executableFile, inFile, timeMs);
        } catch (IOException | InterruptedException | TimeoutException e) {
            LOGGER.info("Ressult: REAL_TIME_LIMIT_EXCEEDED",REAL_TIME_LIMIT_EXCEEDED);
            return REAL_TIME_LIMIT_EXCEEDED; // 超时或系统错误
        }

        // 检查运行结果
        if (runProcess.exitValue() != 0) {
            LOGGER.info("Result: RUNTIME_ERROR", RUNTIME_ERROR);
            return RUNTIME_ERROR; // 运行时错误
        }

        // 验证输出
        try {
            if (!compareOutput(runProcess.getInputStream(), outFile)) {
                LOGGER.info("Result: WRONG_ANSWER", WRONG_ANSWER);
                return WRONG_ANSWER; // 答案错误
            }
        } catch (IOException e) {
            LOGGER.error("Result: SYSTEM_ERROR",SYSTEM_ERROR,"\n",e.getMessage());
            return SYSTEM_ERROR; // 系统错误
        } finally {
            // 删除临时文件
            if (executableFile.exists()) {
                boolean isDeleted = executableFile.delete();
                LOGGER.debug("Deleted executable file: " + executableFile.getAbsolutePath());
                if (!isDeleted) {
                    LOGGER.error("Failed to delete the executable file: " + executableFile.getAbsolutePath());
                }
            }
        }
        LOGGER.info("Result: Accepted", ACCEPTED);
        return ACCEPTED; // 答案正确
    }

    // 判断是否是 Windows 系统
    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    // 编译程序
    private static int compileProgram(File programPath, File executableFile) throws IOException, InterruptedException {
        ProcessBuilder compileBuilder = new ProcessBuilder("g++", "-o", executableFile.getName(), programPath.getAbsolutePath());
        compileBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
        Process compileProcess = compileBuilder.start();

        // 捕获编译输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line); // 打印编译错误信息
            }
        }

        return compileProcess.waitFor(); // 返回编译退出码
    }

    // 运行程序
    private static Process runProgram(File executableFile, File inFile, int timeMs)
            throws IOException, InterruptedException, TimeoutException {
        // 根据操作系统调整可执行文件的调用方式
        String command = isWindows() ? executableFile.getName() : "./" + executableFile.getName();
        ProcessBuilder runBuilder = new ProcessBuilder(command);
        runBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
        Process runProcess = runBuilder.start();

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
            runProcess.destroy(); // 超时，终止进程
            throw new TimeoutException("Process timed out");
        }

        return runProcess;
    }

    // 比较输出
    private static boolean compareOutput(InputStream actualOutput, File expectedOutputFile) throws IOException {
        try (BufferedReader expectedReader = new BufferedReader(new FileReader(expectedOutputFile));
             BufferedReader actualReader = new BufferedReader(new InputStreamReader(actualOutput))) {
            String expectedLine, actualLine;
            while ((expectedLine = expectedReader.readLine()) != null) {
                actualLine = actualReader.readLine();
                if (actualLine == null || !expectedLine.equals(actualLine)) {
                    return false; // 输出不匹配
                }
            }
            return actualReader.readLine() == null; // 检查是否有额外输出
        }
    }
}