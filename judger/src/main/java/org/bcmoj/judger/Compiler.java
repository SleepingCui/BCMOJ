package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Compiler {

    public static int compileProgram(File programPath, File executableFile, boolean enableO2, long timeoutMs) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("g++");
        command.add("-o");
        command.add(executableFile.getName());
        command.add(programPath.getAbsolutePath());
        command.add("-std=c++11");
        if (enableO2) command.add("-O2");

        log.debug("Compile command: {}", command);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        Future<Integer> compileTask = Executors.newSingleThreadExecutor().submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> log.info("[Compiler] {}", line));
            }
            return process.waitFor();
        });

        try {
            return compileTask.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            process.destroyForcibly();
            throw e;
        }
    }
}
