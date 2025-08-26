package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Runner utility for executing compiled programs.
 *
 * <p>This class runs an executable with specified input,
 * enforces a time limit, captures stdout, and returns results.</p>
 *
 * <p>Provides custom TimeoutException in case of exceeding the time limit.</p>
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

        public RunResult(String output, double elapsedTime, int exitCode) {
            this.output = output;
            this.elapsedTime = elapsedTime;
            this.exitCode = exitCode;
        }
    }

    public static RunResult runProgram(File executableFile, String inputContent, int timeLimitMs) throws IOException, InterruptedException, TimeoutException {
        String command;
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean success = executableFile.setExecutable(true);
            if (!success) {
                log.warn("Failed to set executable permission on file: {}", executableFile.getAbsolutePath());
            }
        }
        command = executableFile.getAbsolutePath();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        long startTime = System.nanoTime();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(inputContent);
            writer.flush();
        }

        boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1_000_000.0;

        if (!finished) {
            process.destroyForcibly();
            throw new TimeoutException(elapsedTime);
        }

        String output = readAll(process.getInputStream());
        return new RunResult(output, elapsedTime, process.exitValue());
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
