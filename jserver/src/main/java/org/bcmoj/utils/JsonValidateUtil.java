package org.bcmoj.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Utility class for validating JSON configurations against a predefined JSON Schema.
 * <p>
 * This class loads a JSON Schema from the resource "/problem.schema.json" and validates
 * input JSON strings against it. It also ensures that checkpoint input/output pairs
 * are properly matched (i.e., every "_in" key has a corresponding "_out" key and vice versa).
 * </p>
 * <p>
 * If validation fails, it records the last error result as a JSON string that can be
 * retrieved for reporting or logging purposes.
 * </p>
 */
@Slf4j
@Getter
public class JsonValidateUtil {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static Schema schema;
    private String lastErrorJson = null;

    static {
        try (InputStream in = JsonValidateUtil.class.getResourceAsStream("/problem.schema.json")) {
            if (in == null) {
                throw new IllegalStateException("Schema file not found");
            }
            JSONObject rawSchema = new JSONObject(new JSONTokener(in));
            schema = SchemaLoader.load(rawSchema);
        } catch (Exception e) {
            log.error("Failed to load schema: {}", e.getMessage(), e);
        }
    }

    /**
     * Validates a JSON configuration string against the loaded schema and
     * performs additional checkpoint input/output pair checks.
     *
     * @param jsonConfig JSON configuration string to validate.
     * @return {@code true} if the JSON is valid and all checkpoint pairs are matched; {@code false} otherwise.
     *         On failure, {@link #lastErrorJson} will be set to a JSON string describing the error.
     */
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
            lastErrorJson = JudgeResultUtil.buildResult(null, false, true, 1);
            return false;
        }
    }

    /**
     * Checks that every checkpoint input file has a corresponding output file and vice versa.
     * If the check fails, records the error result in {@link #lastErrorJson}.
     *
     * @param checkpoints JSON node representing the checkpoints object.
     * @return {@code true} if all input/output pairs are matched; {@code false} otherwise.
     */
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

    /**
     * Helper method to set failure state with an error message and generate a default error JSON result.
     *
     * @param message     Error message to log.
     * @param checkpoints Checkpoints node used to determine number of checkpoints.
     * @return always returns {@code false} to indicate failure.
     */
    private boolean fail(String message, JsonNode checkpoints) {
        log.warn(message);
        int count = countInFiles(checkpoints);
        lastErrorJson = JudgeResultUtil.buildResult(null, false, true, count);
        return false;
    }

    /**
     * Counts the number of input checkpoint files (keys ending with "_in").
     *
     * @param checkpoints JSON node representing the checkpoints object.
     * @return Number of input checkpoint files found; minimum 1.
     */
    public int countInFiles(JsonNode checkpoints) {
        int count = 0;
        if (checkpoints != null && checkpoints.isObject()) {
            for (Iterator<String> it = checkpoints.fieldNames(); it.hasNext(); ) {
                if (it.next().endsWith("_in")) count++;
            }
        }
        return Math.max(count, 1);
    }
}
