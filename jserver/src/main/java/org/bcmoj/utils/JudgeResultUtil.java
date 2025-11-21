package org.bcmoj.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Judger;

import java.util.List;

/**
 * Utility class for building JSON-formatted judge result responses.
 * <p>
 * This class generates JSON output for judge results in either of two formats:
 * <ul>
 *     <li>Old format: flat structure with keys like "1_res", "1_time", "1_mem"</li>
 *     <li>New format: structured object with "checkpoints" containing individual result objects</li>
 * </ul>
 * </p>
 * <p>
 * The class handles special cases such as security check failures and system errors,
 * ensuring appropriate status codes are returned for all checkpoints.
 * </p>
 */
@Slf4j
public class JudgeResultUtil {

    /**
     * Builds judge results JSON using the default new format.
     * <p>
     * This is a backward-compatible method that defaults to the new structured format.
     * </p>
     *
     * @param results List of judge results for each checkpoint. Can be null if there was a security check failure.
     * @param isSecurityCheckFailed indicates if the submission failed the security check
     * @param isSystemError indicates if there was a system error during judging
     * @param checkpointsCount total number of checkpoints to report results for
     * @return JSON string containing the judge results in the default format
     */
    public static String buildResult(List<Judger.JudgeResult> results, boolean isSecurityCheckFailed, boolean isSystemError, int checkpointsCount) {
        return buildResult(results, isSecurityCheckFailed, isSystemError, checkpointsCount, false);
    }

    /**
     * Builds judge results JSON in the specified format.
     *
     * @param results List of judge results for each checkpoint. Can be null if there was a security check failure.
     * @param isSecurityCheckFailed indicates if the submission failed the security check
     * @param isSystemError indicates if there was a system error during judging
     * @param checkpointsCount total number of checkpoints to report results for
     * @param useOldFormat if true, generates flat format (1_res, 1_time, 1_mem, etc.);
     *                     if false, generates structured format with checkpoints object
     * @return JSON string containing the judge results in the specified format.
     *         Returns error format if serialization fails.
     */
    public static String buildResult(List<Judger.JudgeResult> results, boolean isSecurityCheckFailed, boolean isSystemError, int checkpointsCount, boolean useOldFormat) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        class CP {
            final int res;
            final double time;
            final long mem;
            CP(int res, double time, long mem) { this.res = res; this.time = time; this.mem = mem; }
        }
        java.util.function.IntFunction<CP> getCP = (i) -> {
            if (isSecurityCheckFailed) {
                return new CP(-5, 0.0, 0L);
            } else if (isSystemError) {
                return new CP(5, 0.0, 0L);
            } else {
                Judger.JudgeResult result = results.get(i);
                return new CP(result.statusCode, result.time, result.maxMemoryUsedKB);
            }
        };

        try {
            if (useOldFormat) {
                for (int i = 0; i < checkpointsCount; i++) {
                    CP cp = getCP.apply(i);
                    root.put((i + 1) + "_res", cp.res);
                    root.put((i + 1) + "_time", cp.time);
                    root.put((i + 1) + "_mem", cp.mem);
                }
            } else {
                ObjectNode checkpointsNode = mapper.createObjectNode();
                for (int i = 0; i < checkpointsCount; i++) {
                    CP cp = getCP.apply(i);
                    ObjectNode single = mapper.createObjectNode();
                    single.put("res", cp.res);
                    single.put("time", cp.time);
                    single.put("mem", cp.mem);
                    checkpointsNode.set(String.valueOf(i + 1), single);
                }
                root.set("checkpoints", checkpointsNode);
            }
            return mapper.writeValueAsString(root);

        } catch (Exception e) {
            log.error("Failed to serialize judge result JSON", e);
            ObjectNode errorNode = mapper.createObjectNode();

            if (useOldFormat) {
                for (int i = 0; i < checkpointsCount; i++) {
                    errorNode.put((i + 1) + "_res", 5);
                    errorNode.put((i + 1) + "_time", 0.0);
                    errorNode.put((i + 1) + "_mem", 0L);
                }
            } else {
                ObjectNode cp = mapper.createObjectNode();
                for (int i = 0; i < checkpointsCount; i++) {
                    ObjectNode single = mapper.createObjectNode();
                    single.put("res", 5);
                    single.put("time", 0.0);
                    single.put("mem", 0L);
                    cp.set(String.valueOf(i + 1), single);
                }
                errorNode.set("checkpoints", cp);
            }
            return errorNode.toString();
        }
    }
}