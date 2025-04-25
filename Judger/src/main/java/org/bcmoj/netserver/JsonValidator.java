package org.bcmoj.netserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class JsonValidator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonValidator.class);

    public boolean validate(String jsonConfig, DataOutputStream dos) throws IOException {
        try {
            JsonNode root = mapper.readTree(jsonConfig);

            // 检查必需字段
            if (!root.has("timeLimit") || !root.has("securityCheck")) {
                logger.warn("Validation Failed - Missing required fields (timeLimit or securityCheck)");
                sendResponse(dos, false, "Missing required fields (timeLimit or securityCheck)");
                return false;
            }
            logger.info("timeLimit and securityCheck - Passed");

            // 检查timeLimit是正整数
            if (!root.get("timeLimit").isInt() || root.get("timeLimit").asInt() <= 0) {
                logger.warn("Validation Failed - timeLimit must be a positive integer");
                sendResponse(dos, false, "timeLimit must be a positive integer");
                return false;
            }
            logger.info("timeLimit is a positive integer - Passed");

            // 检查checkpoints是否存在
            if (!root.has("checkpoints")) {
                logger.warn("Validation Failed - Missing checkpoints field");
                sendResponse(dos, false, "Missing checkpoints field");
                return false;
            }
            logger.info("checkpoints field exists - Passed");

            JsonNode checkpoints = root.get("checkpoints");
            if (!checkpoints.isObject()) {
                logger.warn("Validation Failed - checkpoints must be an object");
                sendResponse(dos, false, "checkpoints must be an object");
                return false;
            }
            logger.info("checkpoints is an object - Passed");

            // 检查每个 _in 文件是否有对应的 _out 文件
            Iterator<String> fieldNames = checkpoints.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (name.endsWith("_in")) {
                    String outName = name.replace("_in", "_out");
                    if (!checkpoints.has(outName)) {
                        logger.warn("Validation Failed - Missing output for input: {}", name);
                        sendResponse(dos, false, "Missing corresponding output for input: " + name);
                        return false;
                    } else {
                        logger.info("{} and {} - Matched", name, outName);
                    }
                }
            }

            logger.info("All validation checks - Passed");
            sendResponse(dos, true, "");
            return true;

        } catch (Exception e) {
            logger.error("Validation Failed - Invalid JSON format: {}", e.getMessage());
            sendResponse(dos, false, "Invalid JSON format: " + e.getMessage());
            return false;
        }
    }

    private void sendResponse(DataOutputStream dos, boolean verified, String reason) throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("verified", verified);
        response.put("reason", reason);
        dos.writeUTF(response.toString());
        dos.flush();
    }
}
