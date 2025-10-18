package org.bcmoj.judger;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.exception.TimeoutException;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Manages Linux cgroups for memory limiting and monitors memory usage of a given process.
 *
 * <p>This class handles the creation and configuration of a cgroup (v1 or v2)
 * to enforce a memory limit on a specified process. It also starts a dedicated
 * thread to monitor the process's memory usage (VmRSS) by reading /proc/<pid>/status.</p>
 *
 * <p>The monitoring thread periodically checks the process's memory and records
 * the peak value observed. The monitoring stops when the process finishes or
 * when explicitly signaled via the {@code processFinished} flag.</p>
 *
 * <p>Resource cleanup (stopping the monitor thread, removing the process from
 * the cgroup, and deleting the cgroup directory) is performed by the
 * {@link #cleanupAndGetMaxMemory()} method.</p>
 *
 * @author SleepingCui
 */
@Slf4j
public class LinuxMemoryLimiter {

    private static final String CGROUP_V1_MEMORY_PATH = "/sys/fs/cgroup/memory";
    private static final String CGROUP_V2_UNIFIED_PATH = "/sys/fs/cgroup";
    private static final Pattern PID_PATTERN = Pattern.compile("\\d+");

    private final long memoryLimitKB;
    private final boolean isV2;
    private final String cgroupName;
    private final String cgroupPath;
    private final String cgroupTasksPath;
    private final Process process;
    private final long pid;

    private Thread memoryMonitorThread;
    private final AtomicBoolean processFinished = new AtomicBoolean(false);
    private final AtomicLong maxMemoryKB = new AtomicLong(0);

    /**
     * Constructs a LinuxMemoryLimiter for the given process.
     *
     * <p>Detects the cgroup version (v1 or v2) used by the system.</p>
     *
     * @param process The process to monitor and limit.
     * @param memoryLimitKB The memory limit to enforce, in kilobytes.
     */
    public LinuxMemoryLimiter(Process process, long memoryLimitKB) {
        this.process = process;
        this.pid = process.pid();
        this.memoryLimitKB = memoryLimitKB;
        this.isV2 = isCgroupV2Available();
        log.debug("Detected cgroup version: {} for PID {}.", isV2 ? "v2" : "v1", pid);

        this.cgroupName = "judger_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        this.cgroupPath = (isV2 ? CGROUP_V2_UNIFIED_PATH : CGROUP_V1_MEMORY_PATH) + "/" + cgroupName;
        this.cgroupTasksPath = isV2 ? cgroupPath + "/cgroup.procs" : cgroupPath + "/tasks";
    }

    /**
     * Sets up the cgroup and starts memory monitoring.
     *
     * <p>This method creates the cgroup directory, sets the memory limit,
     * attempts to add the process to the cgroup, and starts the memory monitoring thread.</p>
     * <p>If the process has already finished before adding it to the cgroup, it logs a warning
     * and skips the cgroup setup and monitoring.</p>
     *
     * @throws IOException If an error occurs during cgroup setup (e.g., creating directory,
     *                     writing limit file) and the process is still alive.
     *                     If the process dies during the PID assignment step, it logs a warning
     *                     and returns without throwing an exception.
     */
    public void setup() throws IOException {
        log.debug("Attempting to set up cgroup '{}' for memory limit {} KB using cgroup v{}.", cgroupName, memoryLimitKB, isV2 ? 2 : 1);
        File cgroupDir = new File(cgroupPath);

        if (!cgroupDir.mkdirs()) {
            throw new IOException("Failed to create cgroup directory: " + cgroupPath + ". Does the path exist and do you have permissions?");
        }

        String limitFilePath = isV2 ? cgroupPath + "/memory.max" : cgroupPath + "/memory.limit_in_bytes";
        String limitValue = String.valueOf(memoryLimitKB * 1024);
        if (memoryLimitKB <= 0) {
            limitValue = "max";
        }

        try (FileWriter limitWriter = new FileWriter(limitFilePath)) {
            limitWriter.write(limitValue);
        }
        log.debug("Set memory limit to {} in cgroup '{}'.", limitValue, cgroupName);
        if (!process.isAlive()) {
            log.warn("Process PID {} has already finished before adding it to cgroup '{}'", pid, cgroupName);
            log.warn("Skipping cgroup assignment and memory monitoring");
            return;
        }

        try (FileWriter tasksWriter = new FileWriter(cgroupTasksPath)) {
            tasksWriter.write(String.valueOf(pid));
        } catch (IOException e) {
            if (!process.isAlive()) {
                log.warn("Process PID {} has already finished before adding it to cgroup '{}'", pid, cgroupName);
                log.warn("Skipping cgroup assignment and memory monitoring");
                return;
            } else {
                log.error("Failed to add process PID {} to cgroup '{}' tasks file '{}'. Process is still alive. Error: {}", pid, cgroupName, cgroupTasksPath, e.getMessage());
                throw e;
            }
        }
        log.debug("Added PID {} to cgroup '{}'.", pid, cgroupName);
        startMemoryMonitoring();
    }

    /**
     * Starts the memory monitoring thread.
     *
     * <p>The thread reads /proc/<pid>/status periodically to find the VmRSS value,
     * compares it with the current maximum, and updates the maximum if a higher value is found.
     * The thread stops monitoring when the process is no longer alive or when
     * {@code processFinished} is set to {@code true}.</p>
     */
    private void startMemoryMonitoring() {
        log.debug("Starting memory monitoring for PID: {}", pid);
        Process finalProcess = this.process;

        memoryMonitorThread = new Thread(() -> {
            String statusFile = "/proc/" + pid + "/status";
            long localMaxMemoryKB = 0;

            while (!processFinished.get()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(statusFile))) {
                    String line;
                    boolean rssFound = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("VmRSS:")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 2) {
                                try {
                                    long rssValue = Long.parseLong(parts[1]);
                                    rssFound = true;
                                    log.trace("PID {}: Current VmRSS: {} KB", pid, rssValue);
                                    if (rssValue > localMaxMemoryKB) {
                                        localMaxMemoryKB = rssValue;
                                    }
                                } catch (NumberFormatException e) {
                                    log.warn("Could not parse memory value from line: '{}'. Error: {}", line, e.getMessage());
                                }
                            }
                            break;
                        }
                    }
                    if (!rssFound) {
                        log.debug("Could not find VmRSS in /proc/{}/status for PID {} at this moment.", pid, pid);
                    }
                } catch (IOException e) {
                    if (finalProcess.isAlive()) {
                        log.warn("Error reading /proc/{}/status: {}", pid, e.getMessage());
                    } else {
                        log.debug("Process PID {} likely finished, stopping memory monitor.", pid);
                        break;
                    }
                }
                try {
                    Thread.sleep(50); // Adjust frequency
                } catch (InterruptedException e) {
                    log.debug("Memory monitor thread for PID {} interrupted.", pid);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            maxMemoryKB.set(localMaxMemoryKB); // Update the shared AtomicLong when loop ends
            log.debug("Memory monitor thread for PID {} finished. Max memory recorded (before set): {} KB", pid, localMaxMemoryKB);
        }, "MemoryMonitor-" + pid);

        memoryMonitorThread.setDaemon(true);
        memoryMonitorThread.start();
    }


    public void waitForProcess(long timeLimitMs) throws InterruptedException, TimeoutException {
        boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            log.info("Process exceeded time limit ({} ms), terminating PID {}.", timeLimitMs, pid);
            process.destroyForcibly();
            throw new TimeoutException((double) (timeLimitMs));
        }
    }

    /**
     * Performs cleanup operations and returns the maximum memory used by the process.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Signals the memory monitoring thread to stop.</li>
     *   <li>Waits for the monitoring thread to finish using {@code join}.</li>
     *   <li>Reads the final maximum memory value recorded by the monitoring thread.</li>
     *   <li>Moves any remaining PIDs out of the cgroup's tasks/procs file.</li>
     *   <li>Deletes the cgroup directory.</li>
     * </ol>
     *
     * <p>This method must be called to ensure proper resource cleanup and
     * to obtain the accurate peak memory usage.</p>
     *
     * @return The maximum memory used by the process, in kilobytes.
     * @throws InterruptedException If the current thread is interrupted while waiting for the monitor thread.
     */
    public long cleanupAndGetMaxMemory() throws InterruptedException {
        log.debug("Cleaning up cgroup '{}' for PID {}.", cgroupName, pid);
        processFinished.set(true);

        if (memoryMonitorThread != null) {
            memoryMonitorThread.interrupt();
            memoryMonitorThread.join(1000); // Wait for monitor to finish
        }

        long finalMaxMemory = maxMemoryKB.get();
        log.debug("Final max memory retrieved from monitor after join: {} KB for PID {}.", finalMaxMemory, pid);
        File cgroupDir = new File(cgroupPath);
        if (cgroupDir.exists()) {
            String tasksPath = this.cgroupTasksPath;
            String rootTasksPath = isV2 ? CGROUP_V2_UNIFIED_PATH + "/cgroup.procs" : CGROUP_V1_MEMORY_PATH + "/tasks";

            try (BufferedReader tasksReader = new BufferedReader(new FileReader(tasksPath))) {
                String line;
                while ((line = tasksReader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && PID_PATTERN.matcher(line).matches()) {
                        try (FileWriter rootTasksWriter = new FileWriter(rootTasksPath)) {
                            rootTasksWriter.write(line);
                            log.trace("Moved PID {} out of cgroup '{}'.", line, cgroupName);
                        } catch (IOException e) {
                            log.debug("Could not move PID {} out of cgroup '{}' during cleanup: {}", line, cgroupName, e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Could not read tasks/procs file '{}' during cleanup: {}", tasksPath, e.getMessage());
            }

            if (!cgroupDir.delete()) {
                log.warn("Failed to delete cgroup directory '{}'.", cgroupPath);
            } else {
                log.debug("Successfully deleted cgroup directory '{}'.", cgroupPath);
            }
        } else {
            log.debug("Cgroup directory '{}' does not exist, skipping deletion.", cgroupPath);
        }
        return finalMaxMemory;
    }

    public int getExitCode() {
        return process.exitValue();
    }

    /**
     * Detects the cgroup version (v1 or v2) used by the system.
     *
     * <p>It reads /proc/self/cgroup and looks for a line starting with '0::/',
     * which indicates the unified cgroup v2 hierarchy.</p>
     *
     * @return {@code true} if cgroup v2 is detected, {@code false} otherwise (assuming v1).
     */
    private static boolean isCgroupV2Available() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/cgroup"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("0::/")) {
                    log.debug("Detected cgroup v2 from /proc/self/cgroup.");
                    return true;
                }
            }
        } catch (IOException e) {
            log.warn("Could not read /proc/self/cgroup, assuming v1. Error: {}", e.getMessage());
        }
        log.debug("Assuming cgroup v1.");
        return false;
    }
}