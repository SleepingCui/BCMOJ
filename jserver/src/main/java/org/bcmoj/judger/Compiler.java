package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class Compiler {

    public static int compileProgram(File programPath, File executableFile, boolean enableO2, long timeoutMs) throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("win") && !executableFile.getName().toLowerCase().endsWith(".exe")) {
            executableFile = new File(executableFile.getAbsolutePath() + ".exe");
        }

        List<String> command = new ArrayList<>();
        command.add("g++");
        command.add("-o");
        command.add(executableFile.getAbsolutePath());
        command.add(programPath.getAbsolutePath());
        command.add("-std=c++11");
        if (enableO2) command.add("-O2");

        log.debug("Compile command: {}", String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> compileTask = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    reader.lines().forEach(line -> log.debug("[Compiler] {}", line));
                }
                int exitCode = process.waitFor();
                log.info("Compilation process exited with code: {}", exitCode);
                return exitCode;
            });
            return compileTask.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Compilation timed out after {} ms, killing process...", timeoutMs);
            process.destroyForcibly();
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }
}
