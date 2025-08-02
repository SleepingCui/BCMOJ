package org.bcmoj.judger;

import org.bcmoj.judger.Judger.JudgeResult;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class JudgerTest {

    private static final String source_code = "#include<iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<a+b<<std::endl;return 0;}";
    private static final String expected_output = "7";
    private static final String input = "3 4";

    //accept
    @Test
    public void testAcceptedSubmission() throws IOException {
        File cppFile = createTempCppFile(source_code);
        JudgeResult result = Judger.judge(cppFile, input, expected_output, 2000);
        assertEquals(Judger.ACCEPTED, result.statusCode);
        assertTrue(result.time >= 0);
    }
    //compile error
    @Test
    public void testCompileError() throws IOException {
        String brokenCode = "#include<iostream>\nint main(){fucku}";
        File cppFile = createTempCppFile(brokenCode);
        JudgeResult result = Judger.judge(cppFile, input, expected_output, 2000);
        assertEquals(Judger.COMPILE_ERROR, result.statusCode);
    }
    //wrong answer
    @Test
    public void testWrongAnswer() throws IOException {
        String wrongAnswerCode = "#include<iostream>\nint main(){std::cout<<42<<std::endl;return 0;}";
        File cppFile = createTempCppFile(wrongAnswerCode);
        JudgeResult result = Judger.judge(cppFile, input, expected_output, 2000);
        assertEquals(Judger.WRONG_ANSWER, result.statusCode);
    }
    //real time limit exceeded
    @Test
    public void testTimeLimitExceeded() throws IOException {
        String timeoutCode = "#include<unistd.h>\nint main(){sleep(3);return 0;}";
        File cppFile = createTempCppFile(timeoutCode);
        JudgeResult result = Judger.judge(cppFile, "", "", 1000);
        assertEquals(Judger.REAL_TIME_LIMIT_EXCEEDED, result.statusCode);
    }
    private File createTempCppFile(String source) throws IOException {
        File tempFile = File.createTempFile("test", ".cpp");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(source);
        }
        tempFile.deleteOnExit();
        return tempFile;
    }
}
