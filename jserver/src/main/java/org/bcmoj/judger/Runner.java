package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.exception.MemoryLimitExceededException;
import org.bcmoj.exception.TimeoutException;

import java.io.*;

/**
 * Runner utility for executing compiled programs.
 *
 * <p>This class runs an executable with specified input,
 * enforces a time limit, captures stdout, and returns results.</p>
 *
 * <p>Provides custom TimeoutException and MemoryLimitExceededException.</p>
 *
 * <p>On Linux, it optionally delegates memory limiting and monitoring to LinuxMemoryLimiter
 * based on the {@code DisableMem} flag.</p>
 * <p>On Windows and other OS, only time limit is enforced.</p>
 *
 * <p>Logging includes execution start, end, and any permission issues.</p>
 *
 * @author SleepingCui
 */
@Slf4j
public class Runner {

    public static class RunResult {
        public final String output;
        public final double elapsedTime;
        public final int exitCode;
        public final long maxMemoryUsedKB;

        public RunResult(String output, double elapsedTime, int exitCode, long maxMemoryUsedKB) {
            this.output = output;
            this.elapsedTime = elapsedTime;
            this.exitCode = exitCode;
            this.maxMemoryUsedKB = maxMemoryUsedKB;
        }
    }

    /**
     * Runs a compiled executable with input, enforcing time and optionally memory limits.
     *
     * @param executableFile The compiled executable file to run.
     * @param inputContent   The input string to provide to the executable.
     * @param timeLimitMs    The time limit in milliseconds.
     * @param memoryLimitKB  The memory limit in kilobytes. (Ignored if {@code DisableMem} is true, or on Windows/other OS)
     * @param DisableMemLimit     Flag to disable memory limiting and monitoring entirely.
     * @return A RunResult containing output, elapsed time, exit code, and max memory used (0 if disabled).
     * @throws IOException        If an I/O error occurs.
     * @throws InterruptedException If the thread is interrupted.
     * @throws TimeoutException   If the process exceeds the time limit.
     * @throws MemoryLimitExceededException If the process exceeds the memory limit (only possible if {@code DisableMem} is false and on Linux).
     */
    public static RunResult runProgram(File executableFile, String inputContent, int timeLimitMs, long memoryLimitKB, boolean DisableMemLimit) throws IOException, InterruptedException, TimeoutException, MemoryLimitExceededException {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isLinux = osName.contains("linux");

        if (!osName.contains("win")) {
            if (!executableFile.setExecutable(true)) {
                log.warn("Failed to set executable permission on file: {}", executableFile.getAbsolutePath());
            }
        }

        ProcessBuilder builder = new ProcessBuilder(executableFile.getAbsolutePath());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        long startTime = System.nanoTime();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(inputContent);
            writer.flush();
        }

        LinuxMemoryLimiter limiter = null;
        String output;
        double elapsedTime;
        int exitCode;
        long finalMaxMemoryKB = 0;

        try {
            if (isLinux && !DisableMemLimit) {
                limiter = new LinuxMemoryLimiter(process, memoryLimitKB);
                limiter.setup();
                limiter.waitForProcess(timeLimitMs);
                exitCode = limiter.getExitCode();

                // Check for common OOM exit code (137 is SIGKILL, often used by OOM killer)
                if (exitCode == 137) {
                    log.info("Process (PID {}) was terminated by the OOM killer ({})", process.pid(), exitCode);
                    elapsedTime = (System.nanoTime() - startTime) / 1_000_000.0;
                    finalMaxMemoryKB = limiter.cleanupAndGetMaxMemory();
                    throw new MemoryLimitExceededException(elapsedTime, finalMaxMemoryKB);
                }

                // If not OOM, proceed with normal flow: read output, get memory, return result
                output = readAll(process.getInputStream());
                elapsedTime = (System.nanoTime() - startTime) / 1_000_000.0;
                finalMaxMemoryKB = limiter.cleanupAndGetMaxMemory();
                log.info("Process (PID {}) finished. Max memory used: {} KB", process.pid(), finalMaxMemoryKB);

            } else {
                if (isLinux) {
                    log.warn("Memory limiting and monitoring are disabled");
                } else {
                    log.warn("Memory limiting is not supported on this OS ({}). Memory limit is ignored", osName);
                }
                boolean finished = process.waitFor(timeLimitMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new TimeoutException(timeLimitMs);
                }
                exitCode = process.exitValue();
                output = readAll(process.getInputStream());
                elapsedTime = (System.nanoTime() - startTime) / 1_000_000.0;
            }
            return new RunResult(output, elapsedTime, exitCode, finalMaxMemoryKB);

        } finally {
            if (limiter != null) {
                try {
                    limiter.cleanupAndGetMaxMemory();
                } catch (InterruptedException e) {
                    log.warn("Linux limiter cleanup in finally interrupted for PID {}", process.pid());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}