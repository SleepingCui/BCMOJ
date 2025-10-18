package org.bcmoj.exception;

import lombok.Getter;

@Getter
public class MemoryLimitExceededException extends Exception {
    private final double elapsedTime;
    private final long maxMemoryUsedKB;

    public MemoryLimitExceededException(double elapsedTime, long maxMemoryUsedKB) {
        super("Memory limit exceeded. Elapsed time: " + elapsedTime + " ms, Max memory used: " + maxMemoryUsedKB + " KB");
        this.elapsedTime = elapsedTime;
        this.maxMemoryUsedKB = maxMemoryUsedKB;
    }
    public MemoryLimitExceededException(double elapsedTime) {
        this(elapsedTime, 0L);
    }

}