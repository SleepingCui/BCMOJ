package org.bcmoj.judgeserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judger.Judger;

import java.util.List;

@Slf4j
public class ResultBuilder {
    public static String buildResult(List<Judger.JudgeResult> results, boolean isSecurityCheckFailed, boolean isSystemError, int checkpointsCount) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        for (int i = 0; i < checkpointsCount; i++) {
            int statusCode;
            double time;
            if (isSecurityCheckFailed || isSystemError) {
                statusCode = 5;
                time = 0.0;
            } else {
                Judger.JudgeResult result = results.get(i);
                statusCode = result.statusCode;
                time = result.time;
            }
            root.put((i + 1) + "_res", statusCode);
            root.put((i + 1) + "_time", time);
        }

        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Failed to serialize judge result JSON", e);
            ObjectNode errorNode = mapper.createObjectNode();
            for (int i = 0; i < checkpointsCount; i++) {
                errorNode.put((i + 1) + "_res", 5);
                errorNode.put((i + 1) + "_time", 0.0);
            }
            return errorNode.toString();
        }
    }
}
