package org.bcmoj.exception;

import lombok.Getter;

@Getter
public class TimeoutException extends Exception {
    private final double elapsedTime;

    public TimeoutException(double elapsedTime) {
        super("TIMEOUT: " + elapsedTime);
        this.elapsedTime = elapsedTime;
    }
}
