package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Compiler utility for C++ programs.
 *
 * <p>This class compiles a given source file to an executable,
 * optionally enabling O2 optimization and enforcing a timeout.</p>
 *
 * <p>Logging includes the full compile command, process PID, working directory,
 * and compilation output with timing information.</p>
 *
 * <p>Handles cross-platform executable file naming for Windows.</p>
 *
 * @author SleepingCui
 */
@Slf4j
public class Compiler {

    /**
     * Compiles a C++ program into an executable file.
     *
     * @param programPath         Path to the C++ source file to be compiled
     * @param executableFile      Path where the compiled executable will be generated
     * @param enableO2            Whether to enable the -O2 optimization flag
     * @param disableSecurityArgs Whether to disable compiler security flags such as
     *                            -D_FORTIFY_SOURCE=2, -fstack-protector-strong, -fno-asm, -fno-builtin, -Wall, -Wextra
     * @param timeoutMs           Maximum time in milliseconds to wait for the compilation process
     * @param compilerPath        Path to the compiler executable; defaults to "g++" if null or empty
     * @param cppStandard         C++ standard version to use (e.g., "c++17", "c++20")
     * @return Exit code of the compilation process (0 indicates success)
     * @throws Exception If an error occurs during compilation or the process times out
     */
    public static int compileProgram(File programPath, File executableFile, boolean enableO2, boolean disableSecurityArgs, long timeoutMs, String compilerPath, String cppStandard) throws Exception {
        String compiler = (compilerPath != null && !compilerPath.isEmpty()) ? compilerPath : "g++";
        List<String> command = new ArrayList<>();
        command.add(compiler);
        command.add("-o");
        command.add(executableFile.getAbsolutePath());
        command.add(programPath.getAbsolutePath());
        command.add("-std=" + cppStandard);
        if (enableO2) command.add("-O2");

        if (!disableSecurityArgs) {
            command.add("-D_FORTIFY_SOURCE=2");
            command.add("-fstack-protector-strong");
            command.add("-fno-asm");
            command.add("-fno-builtin");
            command.add("-Wall");
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                command.add("-Wl,-z,now,-z,relro");
            } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                command.add("-Wl,--dynamicbase"); // ASLR
                command.add("-Wl,--nxcompat"); // DEP
                command.add("-static-libgcc");
                command.add("-static-libstdc++");
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                command.add("-Wl,-bind_at_load");
            } else {
                log.warn("Unknown OS: {} — using only base security flags", System.getProperty("os.name").toLowerCase());
            }
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.directory(programPath.getParentFile());
        long startTime = System.currentTimeMillis();
        Process process = builder.start();
        log.debug("Compilation started: PID={}, WorkDir={}", process.pid(), builder.directory() != null ? builder.directory().getAbsolutePath() : System.getProperty("user.dir"));
        log.debug("Compilation command: {}", String.join(" ", command));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> compileTask = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    reader.lines().forEach(line -> log.info("[Compiler] {}", line));
                }
                int exitCode = process.waitFor();
                long duration = System.currentTimeMillis() - startTime;
                log.info("Compilation finished: exitCode={}, duration={} ms", exitCode, duration);
                return exitCode;
            });
            return compileTask.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdownNow();
        }
    }
}
