package org.bcmoj.judger;

import org.bcmoj.judger.Judger.JudgeResult;
import org.bcmoj.utils.OutputCompareUtil.CompareMode;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class JudgerTest {
    private static final String expected_output = "7";
    private static final String input = "3 4";

    //accepted
    @Test
    public void testAcceptedSubmission() throws IOException {
        File cppFile = createTempCppFile("#include<iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<a+b<<std::endl;return 0;}");
        JudgeResult result = Judger.judge(cppFile, input, expected_output, 2000, false,CompareMode.STRICT);
        assertEquals(Judger.ACCEPTED, result.statusCode);
        assertTrue(result.time >= 0);
    }
    //compile error
    @Test
    public void testCompileError() throws IOException {
        String shitcode = "#include<iostream>\nint main(){fuckyou}";
        File cppFile = createTempCppFile(shitcode);
        JudgeResult result = Judger.judge(cppFile, input, expected_output, 2000, false,CompareMode.STRICT);
        assertEquals(Judger.COMPILE_ERROR, result.statusCode);
    }
    //wrong answer
    @Test
    public void testWrongAnswer() throws IOException {
        String ssshit = "#include<iostream>\nint main(){std::cout<<0721<<std::endl;return 0;}";
        File cppFile = createTempCppFile(ssshit);
        JudgeResult result = Judger.judge(cppFile, input, expected_output, 2000, false,CompareMode.STRICT);
        assertEquals(Judger.WRONG_ANSWER, result.statusCode);
    }
    //real time limit exceeded
    @Test
    public void testTimeLimitExceeded() throws IOException {
        String ciallo = "#include<unistd.h>\nint main(){sleep(3);return 0;}";
        File cppFile = createTempCppFile(ciallo);
        JudgeResult result = Judger.judge(cppFile, "", "", 1000, false,CompareMode.STRICT);
        assertEquals(Judger.REAL_TIME_LIMIT_EXCEEDED, result.statusCode);
    }
    private File createTempCppFile(String source) throws IOException {
        File tempFile = File.createTempFile("test", ".cpp");
        try (FileWriter writer = new FileWriter(tempFile)) {writer.write(source);}
        tempFile.deleteOnExit();
        return tempFile;
    }


}
