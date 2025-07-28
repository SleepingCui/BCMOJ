package org.bcmoj.netserver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class JsonValidatorTest {

    private JsonValidator validator;
    private ByteArrayOutputStream baos;
    private DataOutputStream dos;

    @Before
    public void setUp() {
        validator = new JsonValidator();
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
    }

    @After
    public void tearDown() throws IOException {
        dos.close();
        baos.close();
    }

    @Test
    public void testValidJsonWithCorrectInOutPairs() throws IOException {
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

        boolean result = validator.validate(json, dos);
        assertTrue("Expected validation to pass", result);
        assertEquals(0, baos.size());
    }

    @Test
    public void testInvalidJson_MissingOutPair() throws IOException {
        String json = """
            {
              "timeLimit": 1000,
              "securityCheck": true,
              "checkpoints": {
                "1_in": "input1.txt"
              }
            }
            """;

        boolean result = validator.validate(json, dos);
        assertFalse("Expected validation to fail due to missing _out pair", result);
        byte[] responseBytes = baos.toByteArray();
        assertTrue(responseBytes.length > 0);

        String responseStr = new String(responseBytes, StandardCharsets.UTF_8);
        String jsonResponse = responseStr.substring(4);
        assertTrue(jsonResponse.contains("\"1_res\""));
        assertTrue(jsonResponse.contains("5"));
    }

    @Test
    public void testInvalidJson_MissingInPair() throws IOException {
        String json = """
            {
              "timeLimit": 1000,
              "securityCheck": true,
              "checkpoints": {
                "1_out": "output1.txt"
              }
            }
            """;

        boolean result = validator.validate(json, dos);
        assertFalse(result);
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testInvalidJson_NoPairs() throws IOException {
        String json = """
            {
              "timeLimit": 1000,
              "securityCheck": true,
              "checkpoints": {}
            }
            """;

        boolean result = validator.validate(json, dos);
        assertFalse(result);
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testInvalidJson_SchemaViolation() throws IOException {
        String json = """
            {
              "timeLimit": 0,
              "checkpoints": {
                "1_in": "input1.txt",
                "1_out": "output1.txt"
              }
            }
            """;

        boolean result = validator.validate(json, dos);
        assertFalse(result);
        assertTrue(baos.size() > 0);
    }
}
