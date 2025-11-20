package org.bcmoj.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON reading utility class.
 * <p>
 * This class provides utilities for parsing JSON configurations in either old or new format,
 * extracting relevant parameters such as time limits, memory limits, checkpoints, and other
 * judging settings. It supports backward compatibility by handling both configuration formats.
 * </p>
 */
public class JsonReadUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Configuration class for the old JSON format.
     * <p>
     * Expected format includes fields like {@code timeLimit}, {@code memLimit}, etc.
     * </p>
     */
    public static class OldConfig {
        public int timeLimit;
        public int memLimit;
        public JsonNode checkpoints;
        public boolean securityCheck;
        public boolean enableO2;
        public int compareMode = 1;
    }

    /**
     * Configuration class for the new JSON format.
     * <p>
     * Expected format includes fields like {@code time_limit}, {@code mem_limit}, etc.
     * </p>
     */
    public static class NewConfig {
        public int time_limit;
        public int mem_limit;
        public JsonNode checkpoints;
        public boolean enable_security_check;
        public boolean enable_o2;
        public int compare_mode = 1;
    }

    /**
     * Parses a JSON configuration string and returns the extracted configuration parameters.
     *
     * @param jsonConfig JSON configuration string to parse
     * @param useOldFormat if {@code true}, parses using the old format ({@code timeLimit}, {@code memLimit}, etc.);
     *                     if {@code false}, parses using the new format ({@code time_limit}, {@code mem_limit}, etc.)
     * @return a {@link ConfigResult} object containing the parsed configuration values and checkpoint count
     * @throws Exception if JSON parsing fails or if the JSON structure is invalid
     */
    public static ConfigResult parseConfig(String jsonConfig, boolean useOldFormat) throws Exception {
        JsonNode root = MAPPER.readTree(jsonConfig);

        int timeLimit, memLimit;
        JsonNode checkpoints;
        boolean securityCheck;
        boolean enableO2;
        int compareMode;

        if (useOldFormat) {
            OldConfig config = MAPPER.treeToValue(root, OldConfig.class);
            timeLimit = config.timeLimit;
            memLimit = config.memLimit;
            checkpoints = config.checkpoints;
            securityCheck = config.securityCheck;
            enableO2 = config.enableO2;
            compareMode = config.compareMode;
        } else {
            NewConfig config = MAPPER.treeToValue(root, NewConfig.class);
            timeLimit = config.time_limit;
            memLimit = config.mem_limit;
            checkpoints = config.checkpoints;
            securityCheck = config.enable_security_check;
            enableO2 = config.enable_o2;
            compareMode = config.compare_mode;
        }

        int checkpointsCount;
        if (useOldFormat) {
            checkpointsCount = checkpoints.size() / 2;
        } else {
            checkpointsCount = checkpoints.size();
        }

        return new ConfigResult(timeLimit, memLimit, checkpoints, securityCheck, enableO2, compareMode, checkpointsCount, useOldFormat);
    }

    /**
     * Container class for the results of JSON configuration parsing.
     * <p>
     * This immutable class holds all the extracted configuration parameters
     * and computed values from the JSON parsing process.
     * </p>
     */
    public static class ConfigResult {
        public final int timeLimit;
        public final int memLimit;
        public final JsonNode checkpoints;
        public final boolean securityCheck;
        public final boolean enableO2;
        public final int compareMode;
        public final int checkpointsCount;
        public final boolean useOldFormat;

        public ConfigResult(int timeLimit, int memLimit, JsonNode checkpoints, boolean securityCheck,
                            boolean enableO2, int compareMode, int checkpointsCount, boolean useOldFormat) {
            this.timeLimit = timeLimit;
            this.memLimit = memLimit;
            this.checkpoints = checkpoints;
            this.securityCheck = securityCheck;
            this.enableO2 = enableO2;
            this.compareMode = compareMode;
            this.checkpointsCount = checkpointsCount;
            this.useOldFormat = useOldFormat;
        }
    }
}