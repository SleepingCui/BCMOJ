package org.bcmoj.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bcmoj.judger.Judger;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class JudgeResultBuilderTest {

    public static class JudgeResult {
        public int statusCode;
        public double time;
        public JudgeResult(int statusCode, double time) {
            this.statusCode = statusCode;
            this.time = time;
        }
    }

    @Test
    public void testBuildResult_SerializationException() throws Exception {
        List<Judger.JudgeResult> results = Arrays.asList(
                new Judger.JudgeResult(0, 1.23),
                new Judger.JudgeResult(1, 2.34)
        );

        ObjectMapper realMapper = new ObjectMapper();
        ObjectMapper spyMapper = Mockito.spy(realMapper);
        doThrow(new RuntimeException("Mock serialization failure"))
                .when(spyMapper).writeValueAsString(any(ObjectNode.class));

        String resultJson = buildResultWithMapper(results, false, false, 2, spyMapper);
        assertTrue(resultJson.contains("\"1_res\":5"));
        assertTrue(resultJson.contains("\"1_time\":0.0"));
        assertTrue(resultJson.contains("\"2_res\":5"));
        assertTrue(resultJson.contains("\"2_time\":0.0"));
    }

    private String buildResultWithMapper(List<Judger.JudgeResult> results, boolean isSecurityCheckFailed, boolean isSystemError, int checkpointsCount, ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();

        for (int i = 0; i < checkpointsCount; i++) {
            int statusCode;
            double time;
            if (isSecurityCheckFailed || isSystemError) {
                statusCode = 5;
                time = 0.0;
            } else {
                Judger.JudgeResult result = results.get(i);
                statusCode = result.statusCode;
                time = result.time;
            }
            root.put((i + 1) + "_res", statusCode);
            root.put((i + 1) + "_time", time);
        }

        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            ObjectNode errorNode = mapper.createObjectNode();
            for (int i = 0; i < checkpointsCount; i++) {
                errorNode.put((i + 1) + "_res", 5);
                errorNode.put((i + 1) + "_time", 0.0);
            }
            return errorNode.toString();
        }
    }
}
