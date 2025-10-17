package org.bcmoj.netserver;

import org.bcmoj.utils.JsonValidateUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonValidateUtilTest {

    private JsonValidateUtil validator;

    @Before
    public void setUp() {
        validator = new JsonValidateUtil();
    }

    @Test
    public void testValidJsonWithCorrectInOutPairs() {
        String json = """
            {
              "timeLimit": 1000,
              "memLimit": 256,
              "securityCheck": true,
              "checkpoints": {
                "1_in": "i",
                "1_out": "o",
                "2_in": "i1",
                "2_out": "ggghghghghghgh"
              }
            }
            """;

        boolean result = validator.validate(json);
        assertTrue("Expected validation to pass", result);
        assertNull("No error should be returned", validator.getLastErrorJson());
    }

    @Test
    public void testInvalidJson_MissingOutPair() {
        String json = """
            {
              "timeLimit": 1000,
              "memLimit": 256,
              "securityCheck": true,
              "checkpoints": {
                "1_in": "i"
              }
            }
            """;

        boolean result = validator.validate(json);
        assertFalse("Expected validation to fail due to missing _out pair", result);
        assertNotNull("Expected error result", validator.getLastErrorJson());
    }

    @Test
    public void testInvalidJson_MissingInPair() {
        String json = """
            {
              "timeLimit": 1000,
              "memLimit": 256,
              "securityCheck": true,
              "checkpoints": {
                "1_out": "o"
              }
            }
            """;

        boolean result = validator.validate(json);
        assertFalse("Expected validation to fail due to missing _in pair", result);
        assertNotNull("Expected error result", validator.getLastErrorJson());
    }

    @Test
    public void testInvalidJson_NoPairs() {
        String json = """
            {
              "timeLimit": 1000,
              "memLimit": 256,
              "securityCheck": true,
              "checkpoints": {}
            }
            """;

        boolean result = validator.validate(json);
        assertFalse("Expected validation to fail due to no input/output pairs", result);
        assertNotNull("Expected error result", validator.getLastErrorJson());
    }

    @Test
    public void testInvalidJson_SchemaViolation() {
        String json = """
            {
              "timeLimit": 0,
              "checkpoints": {
                "1_in": "i",
                "1_out": "o"
              }
            }
            """;

        boolean result = validator.validate(json);
        assertFalse("Expected validation to fail due to schema violation", result);
        assertNotNull("Expected error result", validator.getLastErrorJson());
    }
}
