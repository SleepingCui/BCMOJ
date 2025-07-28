package org.bcmoj.netserver;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonValidatorTest {

    private JsonValidator validator;

    @Before
    public void setUp() {
        validator = new JsonValidator();
    }

    @Test
    public void testValidJsonWithCorrectInOutPairs() {
        String json = """
            {
              "timeLimit": 1000,
              "securityCheck": true,
              "checkpoints": {
                "1_in": "input1.txt",
                "1_out": "output1.txt",
                "2_in": "input2.txt",
                "2_out": "output2.txt"
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
              "securityCheck": true,
              "checkpoints": {
                "1_in": "input1.txt"
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
              "securityCheck": true,
              "checkpoints": {
                "1_out": "output1.txt"
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
                "1_in": "input1.txt",
                "1_out": "output1.txt"
              }
            }
            """;

        boolean result = validator.validate(json);
        assertFalse("Expected validation to fail due to schema violation", result);
        assertNotNull("Expected error result", validator.getLastErrorJson());
    }
}
