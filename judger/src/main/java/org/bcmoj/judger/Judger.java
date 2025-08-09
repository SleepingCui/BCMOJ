package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.OutputCompareUtil;
import org.bcmoj.utils.StringUtil;

import java.io.File;
import java.util.Random;

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

    public static JudgeResult judge(File programPath, String inputContent, String expectedOutputContent, int time, boolean enableO2, OutputCompareUtil.CompareMode compareMode) {
        Random random = new Random();
        String programName = "c_" + random.nextInt(1000000);
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            programName += ".exe";
        }
        File executableFile = new File(programName);
        log.info("Compiling program: {} with O2 optimization: {}", programName, enableO2);

        try {
            int compileCode = Compiler.compileProgram(programPath, executableFile, enableO2, 10_000);
            if (compileCode != 0) return new JudgeResult(COMPILE_ERROR, 0.0);
            Runner.RunResult runResult = Runner.runProgram(executableFile, StringUtil.unescapeString(inputContent), time);
            if (runResult.exitCode != 0) return new JudgeResult(RUNTIME_ERROR, runResult.elapsedTime);
            boolean outputMatches = OutputCompareUtil.compare(runResult.output, StringUtil.unescapeString(expectedOutputContent), compareMode);

            return outputMatches ? new JudgeResult(ACCEPTED, runResult.elapsedTime) : new JudgeResult(WRONG_ANSWER, runResult.elapsedTime);

        } catch (Runner.TimeoutException e) {
            return new JudgeResult(REAL_TIME_LIMIT_EXCEEDED, e.getElapsedTime());
        } catch (Exception e) {
            log.error("System error: {}", e.getMessage());
            return new JudgeResult(SYSTEM_ERROR, 0.0);
        } finally {
            if (executableFile.exists() && !executableFile.delete()) {
                log.warn("Failed to delete executable: {}", executableFile.getAbsolutePath());
            }
        }
    }
}
