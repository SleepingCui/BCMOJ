package org.bcmoj.netserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
public class JsonValidator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Schema schema;

    static {
        try (InputStream in = JsonValidator.class.getResourceAsStream("/problem.schema.json")) {
            if (in == null) {
                throw new IllegalStateException("Schema file not found");
            }
            JSONObject rawSchema = new JSONObject(new JSONTokener(in));
            schema = SchemaLoader.load(rawSchema);
        } catch (Exception e) {
            log.error("Failed to load schema: {}", e.getMessage());
        }
    }

    public boolean validate(String jsonConfig, DataOutputStream dos) throws IOException {
        try {
            JsonNode root = mapper.readTree(jsonConfig);
            JSONObject jsonObj = new JSONObject(mapper.writeValueAsString(root));
            schema.validate(jsonObj);

            JsonNode checkpoints = root.get("checkpoints");
            boolean inOutOk = checkInOutPairs(checkpoints, dos);
            if (inOutOk) {
                log.info("JSON validation passed");
            }
            return inOutOk;
        } catch (Exception e) {
            return fail("Validation failed: " + e.getMessage(), dos, null);
        }
    }


    private boolean fail(String message, DataOutputStream dos, JsonNode checkpoints) throws IOException {
        log.warn(message);
        sendErrorResponse(dos, checkpoints);
        return false;
    }

    private boolean checkInOutPairs(JsonNode checkpoints, DataOutputStream dos) throws IOException {
        int inCount = 0;
        int outCount = 0;
        Iterator<String> fieldNames = checkpoints.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            if (name.endsWith("_in")) {
                inCount++;
                String outName = name.replace("_in", "_out");
                if (!checkpoints.has(outName)) {
                    return fail("Missing output file for input: " + name, dos, checkpoints);
                }
            } else if (name.endsWith("_out")) {
                outCount++;
                String inName = name.replace("_out", "_in");
                if (!checkpoints.has(inName)) {
                    return fail("Missing input file for output: " + name, dos, checkpoints);
                }
            }
        }
        if (inCount == 0) {
            return fail("At least one _in/_out pair is required", dos, checkpoints);
        }
        if (inCount != outCount) {
            return fail("Mismatch between _in and _out files (in: " + inCount + ", out: " + outCount + ")", dos, checkpoints);
        }
        return true;
    }


    private void sendErrorResponse(DataOutputStream dos, JsonNode checkpoints) throws IOException {
        ObjectNode errorResponse = mapper.createObjectNode();
        int count = 0;
        if (checkpoints != null && checkpoints.isObject()) {
            for (Iterator<String> it = checkpoints.fieldNames(); it.hasNext(); ) {
                if (it.next().endsWith("_in")) count++;
            }
        }
        count = Math.max(count, 1);
        for (int i = 1; i <= count; i++) {
            errorResponse.put(i + "_res", 5);
            errorResponse.put(i + "_time", 0.0);
        }

        String responseStr = errorResponse.toString();
        byte[] data = responseStr.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
        log.info("Response sent to client ({} bytes):\n{}", data.length, responseStr);
    }

}
