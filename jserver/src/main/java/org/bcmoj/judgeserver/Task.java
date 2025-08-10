package org.bcmoj.judgeserver;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Judger;
import org.bcmoj.utils.OutputCompareUtil;

import java.io.File;
import java.util.concurrent.Callable;

@Slf4j
public class Task implements Callable<Judger.JudgeResult> {

    private final File cppFilePath;
    private final String inputContent;
    private final String outputContent;
    private final int timeLimit;
    private final boolean enableO2;
    private final OutputCompareUtil.CompareMode compareMode;
    private final boolean securityCheckFailed;

    public Task(File cppFilePath, String inputContent, String outputContent, int timeLimit, boolean enableO2, OutputCompareUtil.CompareMode compareMode, boolean securityCheckFailed) {
        this.cppFilePath = cppFilePath;
        this.inputContent = inputContent;
        this.outputContent = outputContent;
        this.timeLimit = timeLimit;
        this.enableO2 = enableO2;
        this.compareMode = compareMode;
        this.securityCheckFailed = securityCheckFailed;
    }

    @Override
    public Judger.JudgeResult call() {
        if (securityCheckFailed) {
            return new Judger.JudgeResult(-5, 0.0);
        }
        try {
            return Judger.judge(cppFilePath, inputContent, outputContent, timeLimit, enableO2, compareMode);
        } catch (Exception e) {
            log.error("JudgeTask exception: {}", e.getMessage());
            return new Judger.JudgeResult(5, 0.0);
        }
    }
}
