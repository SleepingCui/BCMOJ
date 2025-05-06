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
import java.util.ArrayList;
import java.util.List;

public class JsonValidator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonValidator.class);

    public boolean validate(String jsonConfig, DataOutputStream dos) throws IOException {
        logger.debug("Starting JSON config validation...");
        try {
            JsonNode root = mapper.readTree(jsonConfig);
            logger.debug("JSON parsed successfully");

            if (!root.has("timeLimit") || !root.has("securityCheck")) {
                logger.warn("Validation failed: missing required fields [timeLimit, securityCheck]");
                sendErrorResponse(dos, root.path("checkpoints"));
                return false;
            }

            if (!root.get("timeLimit").isInt()) {
                logger.warn("Validation failed: 'timeLimit' is not an integer");
                sendErrorResponse(dos, root.path("checkpoints"));
                return false;
            }

            int timeLimit = root.get("timeLimit").asInt();
            logger.debug("Parsed timeLimit: {}", timeLimit);
            if (timeLimit <= 0) {
                logger.warn("Validation failed: timeLimit must be > 0");
                sendErrorResponse(dos, root.path("checkpoints"));
                return false;
            }

            if (!root.has("checkpoints")) {
                logger.warn("Validation failed: 'checkpoints' field missing");
                sendErrorResponse(dos, null);
                return false;
            }

            JsonNode checkpoints = root.get("checkpoints");
            if (!checkpoints.isObject()) {
                logger.warn("Validation failed: 'checkpoints' is not a JSON object");
                sendErrorResponse(dos, checkpoints);
                return false;
            }

            logger.debug("Validating input-output checkpoint pairs...");
            Iterator<String> fieldNames = checkpoints.fieldNames();
            List<String> matchedPairs = new ArrayList<>();

            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (name.endsWith("_in")) {
                    String outName = name.replace("_in", "_out");
                    if (!checkpoints.has(outName)) {
                        logger.warn("Validation failed: Missing output checkpoint for input '{}'", name);
                        sendErrorResponse(dos, checkpoints);
                        return false;
                    } else {
                        matchedPairs.add(name);
                    }
                }
            }

            if (!matchedPairs.isEmpty()) {
                logger.debug("Found matching outputs for inputs: {}", matchedPairs);
            }


            return true;

        } catch (Exception e) {
            logger.error("Validation failed: invalid JSON format - {}", e.getMessage());
            sendErrorResponse(dos, null);
            return false;
        }
    }

    private void sendErrorResponse(DataOutputStream dos, JsonNode checkpointsNode) throws IOException {
        logger.debug("Preparing error response for invalid JSON config...");
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
            logger.debug("Estimated number of test cases from checkpoints: {}", testCaseCount);
        }

        testCaseCount = Math.max(testCaseCount, 1);
        logger.debug("Total test cases to report as failed: {}", testCaseCount);

        for (int i = 1; i <= testCaseCount; i++) {
            errorResponse.put(i + "_res", 5);      // 5 = validation error
            errorResponse.put(i + "_time", 0.0);
        }

        String response = errorResponse.toString();
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(responseBytes.length);
        dos.write(responseBytes);
        dos.flush();

        logger.info("Sent error response to client: {}", response);
    }
}
