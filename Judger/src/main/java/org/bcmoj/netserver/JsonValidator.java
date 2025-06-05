package org.bcmoj.netserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
/**
 * JSON Configuration Validator
 * <p>
 * Validates the structure and content of judge configuration JSON payloads.
 * Ensures required fields are present and properly formatted before processing.
 * </p>
 *
 * <p><b>Validation Rules:</b></p>
 * <ul>
 *   <li>Requires <code>timeLimit</code> (positive integer)</li>
 *   <li>Requires <code>securityCheck</code> boolean field</li>
 *   <li>Requires <code>checkpoints</code> object with matching input/output pairs</li>
 *   <li>Ensures all test inputs (<code>*_in</code>) have corresponding outputs (<code>*_out</code>)</li>
 * </ul>
 *
 * <p><b>Error Response Format:</b></p>
 * <pre>{
 *   "1_res": 5,
 *   "1_time": 0.0,
 *   ...
 * }</pre>
 * <p>Where status code 5 indicates a SYSTEM_ERROR.</p>
 *
 * @author SleepingCui
 * @version 1.0-SNAPSHOT
 * @since 2025
 * @see <a href="https://github.com/SleepingCui/bcmoj-judge-server">GitHub Repository</a>
 */
public class JsonValidator {
    private static final ObjectMapper mapper = new ObjectMapper();
    public boolean validate(String jsonConfig, DataOutputStream dos, Logger logger) throws IOException {
        try {
            JsonNode root = mapper.readTree(jsonConfig);
            if (!root.has("timeLimit") || !root.has("securityCheck")) {
                logger.warn("[JsonValidator] Validation Failed - Missing required fields");
                sendErrorResponse(dos, root.path("checkpoints"), logger);
                return false;
            }
            if (!root.get("timeLimit").isInt() || root.get("timeLimit").asInt() <= 0) {
                logger.warn("[JsonValidator] Validation Failed - Invalid timeLimit");
                sendErrorResponse(dos, root.path("checkpoints"), logger);
                return false;
            }
            if (!root.has("checkpoints")) {
                logger.warn("[JsonValidator] Validation Failed - Missing checkpoints");
                sendErrorResponse(dos, null, logger);
                return false;
            }
            JsonNode checkpoints = root.get("checkpoints");
            if (!checkpoints.isObject()) {
                logger.warn("[JsonValidator] Validation Failed - Invalid checkpoints format");
                sendErrorResponse(dos, checkpoints, logger);
                return false;
            }
            Iterator<String> fieldNames = checkpoints.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (name.endsWith("_in")) {
                    String outName = name.replace("_in", "_out");
                    if (!checkpoints.has(outName)) {
                        logger.warn("[JsonValidator] Validation Failed - Missing output for input: {}", name);
                        sendErrorResponse(dos, checkpoints, logger);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.warn("[JsonValidator] Validation Failed - Invalid JSON format: {}", e.getMessage());
            sendErrorResponse(dos, null, logger);
            return false;
        }
    }

    private void sendErrorResponse(DataOutputStream dos, JsonNode checkpointsNode, Logger logger) throws IOException {
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
        logger.info("Response sent to client ({} bytes)", responseBytes.length);
    }

}
