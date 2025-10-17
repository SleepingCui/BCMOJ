package org.bcmoj.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Judger;

import java.util.List;

/**
 * Utility class for building JSON-formatted judge result responses.
 */
@Slf4j
public class JudgeResultUtil {

    /**
     * Builds a JSON string representing judge results for multiple checkpoints.
     * <p>
     * If security check failed or a system error occurred, all results
     * will have status code 5, time 0, and memory 0. Otherwise, results are serialized
     * based on the provided list.
     * </p>
     *
     * @param results               List of judge results, each representing a checkpoint result.
     * @param isSecurityCheckFailed Flag indicating if the security check failed.
     * @param isSystemError         Flag indicating if a system error occurred.
     * @param checkpointsCount      Number of checkpoints to include in the result.
     * @return JSON string representing the judge results, with keys in the format "index_res", "index_time", and "index_mem".
     *         Returns a JSON with all status code 5, time 0, and mem 0 if serialization fails.
     */
    public static String buildResult(List<Judger.JudgeResult> results, boolean isSecurityCheckFailed, boolean isSystemError, int checkpointsCount) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        for (int i = 0; i < checkpointsCount; i++) {
            int statusCode;
            double time;
            long memory;
            if (isSecurityCheckFailed) {
                statusCode = -5;
                time = 0.0;
                memory = 0L;
            } else if (isSystemError) {
                statusCode = 5;
                time = 0.0;
                memory = 0L;
            } else {
                Judger.JudgeResult result = results.get(i);
                statusCode = result.statusCode;
                time = result.time;
                memory = result.maxMemoryUsedKB;
            }
            root.put((i + 1) + "_res", statusCode);
            root.put((i + 1) + "_time", time);
            root.put((i + 1) + "_mem", memory);
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Failed to serialize judge result JSON", e);
            ObjectNode errorNode = mapper.createObjectNode();
            for (int i = 0; i < checkpointsCount; i++) {
                errorNode.put((i + 1) + "_res", 5);
                errorNode.put((i + 1) + "_time", 0.0);
                errorNode.put((i + 1) + "_mem", 0L);
            }
            return errorNode.toString();
        }
    }
}