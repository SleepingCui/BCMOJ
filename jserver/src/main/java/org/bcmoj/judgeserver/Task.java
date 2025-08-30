package org.bcmoj.judgeserver;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Judger;
import org.bcmoj.utils.OutputCompareUtil;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Task represents a single judge execution for a specific checkpoint (input/output pair).
 *
 * <p>Implements {@link Callable} to allow concurrent execution in thread pools.
 * Handles security check failure by immediately returning the appropriate status code.</p>
 *
 * to perform compilation, execution, and output comparison.</p>
 *
 * Status codes used:
 * <ul>
 *     <li>-5: Security Check Failed</li>
 *     <li>5: System Error</li>
 *     <li>Other codes: see {@link Judger}</li>
 * </ul>
 *
 * Author: SleepingCui
 */
@Slf4j
public class Task implements Callable<Judger.JudgeResult> {

    private final File cppFilePath;
    private final String compilerPath;
    private final String cppStandard;
    private final String inputContent;
    private final String outputContent;
    private final int timeLimit;
    private final boolean enableO2;
    private final OutputCompareUtil.CompareMode compareMode;
    private final boolean securityCheckFailed;

    public Task(File cppFilePath, String compilerPath, String cppStandard, String inputContent, String outputContent, int timeLimit, boolean enableO2, OutputCompareUtil.CompareMode compareMode, boolean securityCheckFailed) {
        this.cppFilePath = cppFilePath;
        this.cppStandard = cppStandard;
        this.inputContent = inputContent;
        this.outputContent = outputContent;
        this.timeLimit = timeLimit;
        this.enableO2 = enableO2;
        this.compareMode = compareMode;
        this.securityCheckFailed = securityCheckFailed;
        this.compilerPath = compilerPath;
    }

    /**
     * Executes the judging task.
     *
     * <p>If security check has failed, immediately returns status code -5.
     * Otherwise, invokes {@link Judger#judge} to compile, run, and compare output.</p>
     *
     * @return {@link Judger.JudgeResult} containing status code and execution time
     */
    @Override
    public Judger.JudgeResult call() {
        if (securityCheckFailed) {
            return new Judger.JudgeResult(-5, 0.0);
        }
        try {
            return Judger.judge(cppFilePath, compilerPath, cppStandard, inputContent, outputContent, timeLimit, enableO2, compareMode);
        } catch (Exception e) {
            log.error("JudgeTask exception: {}", e.getMessage());
            return new Judger.JudgeResult(5, 0.0);
        }
    }
}
