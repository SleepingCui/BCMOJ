package org.bcmoj.judgeserver;

import org.bcmoj.judger.Judger;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static void JudgeServer(File programPath, File inFile, File outFile, int timeLimit, int Checkpoints) {
        // 线程池
        ExecutorService executor = Executors.newFixedThreadPool(Checkpoints);

        // 获取线程的返回结果
        List<Future<Judger.JudgeResult>> futures = new ArrayList<>();

        for (int i = 0; i < Checkpoints; i++) {
            Callable<Judger.JudgeResult> task = () -> Judger.judge(programPath, inFile, outFile, timeLimit);
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
                e.printStackTrace();
                results.add(new Judger.JudgeResult(SYSTEM_ERROR, 0.0)); // 如果出现异常，默认返回 SYSTEM_ERROR
            }
        }

        // 输出所有线程的结果
        LOGGER.info("========== Results ==========");
        for (int i = 0; i < results.size(); i++) {
            Judger.JudgeResult result = results.get(i);
            String status = getStatusDescription(result.statusCode);
            LOGGER.info("Checkpoint " + (i + 1) + " result: " + result.statusCode + " (" + status + "), Time: " + result.timeMs + "ms");
        }
    }

    // 根据状态码返回对应的描述
    private static String getStatusDescription(int statusCode) {
        switch (statusCode) {
            case COMPILE_ERROR:
                return "Compile Error";
            case WRONG_ANSWER:
                return "Wrong Answer";
            case REAL_TIME_LIMIT_EXCEEDED:
                return "Real Time Limit Exceeded";
            case RUNTIME_ERROR:
                return "Runtime Error";
            case SYSTEM_ERROR:
                return "System Error";
            case ACCEPTED:
                return "Accepted";
            default:
                return "Unknown Status";
        }
    }

    @Test
    public void test() throws Exception {
        JudgeServer(new File("D:\\UserData\\Mxing\\Desktop\\aa.cpp"), new File("D:\\UserData\\Mxing\\Desktop\\in.txt"), new File("D:\\UserData\\Mxing\\Desktop\\out.txt"), 1000, 4);
    }
}