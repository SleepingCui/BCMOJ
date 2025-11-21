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
    private static Schema oldSchema;
    private String lastErrorJson = null;

    static {
        try (InputStream in = JsonValidateUtil.class.getResourceAsStream("/problem.schema.json")) {
            if (in == null) {
                throw new IllegalStateException("New schema file not found");
            }
            JSONObject rawSchema = new JSONObject(new JSONTokener(in));
            schema = SchemaLoader.load(rawSchema);
        } catch (Exception e) {
            log.error("Failed to load new schema: {}", e.getMessage(), e);
        }

        try (InputStream in = JsonValidateUtil.class.getResourceAsStream("/problem.schema.old.json")) {
            if (in == null) {
                throw new IllegalStateException("Old schema file not found");
            }
            JSONObject rawSchema = new JSONObject(new JSONTokener(in));
            oldSchema = SchemaLoader.load(rawSchema);
        } catch (Exception e) {
            log.error("Failed to load old schema: {}", e.getMessage(), e);
        }
    }

    /**
     * Validates a JSON configuration string against the appropriate schema based on format type
     * and performs additional checkpoint input/output pair checks.
     *
     * @param jsonConfig JSON configuration string to validate.
     * @param useOldFormat if true, uses the old schema format; if false, uses the new schema format
     * @return {@code true} if the JSON is valid and all checkpoint pairs are matched; {@code false} otherwise.
     *         On failure, {@link #lastErrorJson} will be set to a JSON string describing the error.
     */
    public boolean validate(String jsonConfig, boolean useOldFormat) {
        try {
            JsonNode root = mapper.readTree(jsonConfig);
            JSONObject jsonObj = new JSONObject(mapper.writeValueAsString(root));
            Schema selectedSchema = useOldFormat ? oldSchema : schema;
            selectedSchema.validate(jsonObj);
            JsonNode checkpoints = root.get("checkpoints");
            boolean inOutOk = useOldFormat ? checkInOutPairsOldFormat(checkpoints) : checkInOutPairsNewFormat(checkpoints);
            if (inOutOk) {
                log.info("JSON validation passed (format: {})", useOldFormat ? "old" : "new");
            }
            return inOutOk;
        } catch (Exception e) {
            log.error("Schema validation failed", e);
            lastErrorJson = JudgeResultUtil.buildResult(null, false, true, 1);
            return false;
        }
    }

    /**
     * Validates using the new format (default behavior without useOldFormat parameter)
     * @param jsonConfig JSON configuration string to validate.
     * @return {@code true} if the JSON is valid and all checkpoint pairs are matched; {@code false} otherwise.
     */
    public boolean validate(String jsonConfig) {
        return validate(jsonConfig, false); // 默认使用新格式
    }

    /**
     * Checks that every checkpoint input file has a corresponding output file and vice versa
     * for the old format (1_in, 1_out, 2_in, 2_out...).
     *
     * @param checkpoints JSON node representing the checkpoints object.
     * @return {@code true} if all input/output pairs are matched; {@code false} otherwise.
     */
    private boolean checkInOutPairsOldFormat(JsonNode checkpoints) {
        if (checkpoints == null || !checkpoints.isObject()) {
            return fail("Checkpoints must be an object", checkpoints, true);
        }
        int inCount = 0;
        int outCount = 0;
        Iterator<String> fieldNames = checkpoints.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            if (name.endsWith("_in")) {
                inCount++;
                String outName = name.replace("_in", "_out");
                if (!checkpoints.has(outName)) {
                    return fail("Missing output file for input: " + name, checkpoints, true);
                }
            } else if (name.endsWith("_out")) {
                outCount++;
                String inName = name.replace("_out", "_in");
                if (!checkpoints.has(inName)) {
                    return fail("Missing input file for output: " + name, checkpoints, true);
                }
            }
        }
        if (inCount == 0) {
            return fail("At least one _in/_out pair is required", checkpoints, true);
        }
        if (inCount != outCount) {
            return fail("Mismatch between _in and _out files (in: " + inCount + ", out: " + outCount + ")", checkpoints, true);
        }
        return true;
    }

    /**
     * Checks that every checkpoint has both 'in' and 'out' properties for the new format
     * {"1": {"in": "...", "out": "..."}, "2": {"in": "...", "out": "..."}}.
     *
     * @param checkpoints JSON node representing the checkpoints object.
     * @return {@code true} if all checkpoints have both 'in' and 'out' properties; {@code false} otherwise.
     */
    private boolean checkInOutPairsNewFormat(JsonNode checkpoints) {
        if (checkpoints == null || !checkpoints.isObject()) {
            return fail("Checkpoints must be an object", checkpoints, false);
        }

        int checkpointCount = 0;
        Iterator<String> fieldNames = checkpoints.fieldNames();
        while (fieldNames.hasNext()) {
            String checkpointId = fieldNames.next();
            JsonNode checkpoint = checkpoints.get(checkpointId);

            if (checkpoint == null || !checkpoint.isObject()) {
                return fail("Checkpoint " + checkpointId + " must be an object", checkpoints, false);
            }

            if (!checkpoint.has("in") || !checkpoint.has("out")) {
                return fail("Checkpoint " + checkpointId + " must have both 'in' and 'out' properties", checkpoints, false);
            }

            if (!checkpoint.get("in").isTextual() || !checkpoint.get("out").isTextual()) {
                return fail("Checkpoint " + checkpointId + " 'in' and 'out' properties must be strings", checkpoints, false);
            }

            checkpointCount++;
        }
        if (checkpointCount == 0) {
            return fail("At least one checkpoint is required", checkpoints, false);
        }
        return true;
    }

    /**
     * Helper method to set failure state with an error message and generate a default error JSON result.
     *
     * @param message     Error message to log.
     * @param checkpoints Checkpoints node used to determine number of checkpoints.
     * @param useOldFormat Whether to use old format for counting
     * @return always returns {@code false} to indicate failure.
     */
    private boolean fail(String message, JsonNode checkpoints, boolean useOldFormat) {
        log.warn(message);
        int count = checkpoints != null ? countIns(checkpoints, useOldFormat) : 1;
        lastErrorJson = JudgeResultUtil.buildResult(null, false, true, count);
        return false;
    }

    /**
     * Counts the number of input checkpoint files based on the format type.
     *
     * @param checkpoints JSON node representing the checkpoints object.
     * @param useOldFormat if true, counts "_in" keys; if false, counts checkpoint objects
     * @return Number of input checkpoint files found; minimum 1.
     */
    public int countIns(JsonNode checkpoints, boolean useOldFormat) {
        if (checkpoints == null || !checkpoints.isObject()) {
            return 1;
        }
        int count = 0;
        if (useOldFormat) {
            for (Iterator<String> it = checkpoints.fieldNames(); it.hasNext(); ) {
                if (it.next().endsWith("_in")) {
                    count++;
                }
            }
        } else {
            count = checkpoints.size();
        }
        return Math.max(count, 1);
    }

    /**
     * Counts the number of input checkpoint files using default format (new format).
     *
     * @param checkpoints JSON node representing the checkpoints object.
     * @return Number of input checkpoint files found; minimum 1.
     */
    public int countIns(JsonNode checkpoints) {
        return countIns(checkpoints, false);
    }
}