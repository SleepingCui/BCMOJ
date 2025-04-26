package org.bcmoj.netserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class JsonValidator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonValidator.class);

    public boolean validate(String jsonConfig, DataOutputStream dos) throws IOException {
        try {
            JsonNode root = mapper.readTree(jsonConfig);
            if (!root.has("timeLimit") || !root.has("securityCheck")) {
                logger.warn("Validation Failed - Missing required fields");
                sendErrorResponse(dos, root.path("checkpoints"));
                return false;
            }
            if (!root.get("timeLimit").isInt() || root.get("timeLimit").asInt() <= 0) {
                logger.warn("Validation Failed - Invalid timeLimit");
                sendErrorResponse(dos, root.path("checkpoints"));
                return false;
            }
            if (!root.has("checkpoints")) {
                logger.warn("Validation Failed - Missing checkpoints");
                sendErrorResponse(dos, null);
                return false;
            }
            JsonNode checkpoints = root.get("checkpoints");
            if (!checkpoints.isObject()) {
                logger.warn("Validation Failed - Invalid checkpoints format");
                sendErrorResponse(dos, checkpoints);
                return false;
            }
            Iterator<String> fieldNames = checkpoints.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (name.endsWith("_in")) {
                    String outName = name.replace("_in", "_out");
                    if (!checkpoints.has(outName)) {
                        logger.warn("Validation Failed - Missing output for input: {}", name);
                        sendErrorResponse(dos, checkpoints);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Validation Failed - Invalid JSON format: {}", e.getMessage());
            sendErrorResponse(dos, null);
            return false;
        }
    }
    private void sendErrorResponse(DataOutputStream dos, JsonNode checkpointsNode) throws IOException {
        ObjectNode errorResponse = mapper.createObjectNode();
        int testCaseCount = 0;
        if (checkpointsNode != null && checkpointsNode.isObject()) {
            Iterator<String> fields = checkpointsNode.fieldNames();
            while (fields.hasNext()) {
                String name = fields.next();
                if (name.endsWith("_in")) {
                    testCaseCount++;
                }
            }
        }
        testCaseCount = Math.max(testCaseCount, 1);
        for (int i = 1; i <= testCaseCount; i++) {
            errorResponse.put(i + "_res", 5);
            errorResponse.put(i + "_time", 0.0);
        }
        String response = errorResponse.toString();
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(responseBytes.length);
        dos.write(responseBytes);
        dos.flush();
        logger.info("Sent validation error response: {}", response);
    }
}
