package org.bcmoj.netserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.JudgeResultBuilder;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.Iterator;

@Getter
@Slf4j
public class JsonValidator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Schema schema;
    private String lastErrorJson = null;

    static {
        try (InputStream in = JsonValidator.class.getResourceAsStream("/problem.schema.json")) {
            if (in == null) {
                throw new IllegalStateException("Schema file not found");
            }
            JSONObject rawSchema = new JSONObject(new JSONTokener(in));
            schema = SchemaLoader.load(rawSchema);
        } catch (Exception e) {
            log.error("Failed to load schema: {}", e.getMessage(), e);
        }
    }

    public boolean validate(String jsonConfig) {
        try {
            JsonNode root = mapper.readTree(jsonConfig);
            JSONObject jsonObj = new JSONObject(mapper.writeValueAsString(root));
            schema.validate(jsonObj);

            JsonNode checkpoints = root.get("checkpoints");
            boolean inOutOk = checkInOutPairs(checkpoints);
            if (inOutOk) {
                log.info("JSON validation passed");
            }
            return inOutOk;
        } catch (Exception e) {
            log.error("Schema validation failed", e);
            lastErrorJson = JudgeResultBuilder.buildResult(
                    null, false, true, 1
            );
            return false;
        }
    }

    private boolean fail(String message, JsonNode checkpoints) {
        log.warn(message);
        int count = countInFiles(checkpoints);
        lastErrorJson = JudgeResultBuilder.buildResult(null, false, true, count);
        return false;
    }

    private boolean checkInOutPairs(JsonNode checkpoints) {
        int inCount = 0;
        int outCount = 0;
        Iterator<String> fieldNames = checkpoints.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            if (name.endsWith("_in")) {
                inCount++;
                String outName = name.replace("_in", "_out");
                if (!checkpoints.has(outName)) {
                    return fail("Missing output file for input: " + name, checkpoints);
                }
            } else if (name.endsWith("_out")) {
                outCount++;
                String inName = name.replace("_out", "_in");
                if (!checkpoints.has(inName)) {
                    return fail("Missing input file for output: " + name, checkpoints);
                }
            }
        }
        if (inCount == 0) {
            return fail("At least one _in/_out pair is required", checkpoints);
        }
        if (inCount != outCount) {
            return fail("Mismatch between _in and _out files (in: " + inCount + ", out: " + outCount + ")", checkpoints);
        }
        return true;
    }

    private int countInFiles(JsonNode checkpoints) {
        int count = 0;
        if (checkpoints != null && checkpoints.isObject()) {
            for (Iterator<String> it = checkpoints.fieldNames(); it.hasNext(); ) {
                if (it.next().endsWith("_in")) count++;
            }
        }
        return Math.max(count, 1);
    }
}
