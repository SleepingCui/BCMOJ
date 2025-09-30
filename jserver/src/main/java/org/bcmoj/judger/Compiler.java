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
 * <p>Logging includes the full compile command and compilation output.</p>
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
        if (System.getProperty("os.name").toLowerCase().contains("win") && !executableFile.getName().toLowerCase().endsWith(".exe")) {
            executableFile = new File(executableFile.getAbsolutePath() + ".exe");
        }
        String compiler = (compilerPath != null && !compilerPath.isEmpty()) ? compilerPath : "g++";
        List<String> command = new ArrayList<>();
        command.add(compiler);
        command.add("-o");
        command.add(executableFile.getAbsolutePath());
        command.add(programPath.getAbsolutePath());
        command.add("-std=" + cppStandard);
        if (!disableSecurityArgs) {
            command.add("-D_FORTIFY_SOURCE=2");
            command.add("-fstack-protector-strong");
            command.add("-fno-asm");
            command.add("-fno-builtin");
            command.add("-Wall");
            command.add("-Wextra");
        }
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
        } finally {
            executor.shutdownNow();
        }
    }

}
