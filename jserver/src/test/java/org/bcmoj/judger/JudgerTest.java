package org.bcmoj.judger;

import lombok.SneakyThrows;
import org.bcmoj.exception.MemoryLimitExceededException;
import org.bcmoj.judger.Judger.JudgeResult;
import org.bcmoj.utils.OutputCompareUtil.CompareMode;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class JudgerTest {
    private static final String expected_output = "7";
    private static final String input = "3 4";
    private static final long MEMORY_LIMIT_KB = 1000L;

    //accepted
    @Test
    public void testAcceptedSubmission() throws IOException, MemoryLimitExceededException {
        File cppFile = createTempCppFile("#include<iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<a+b<<std::endl;return 0;}");
        File exeFile = compileCppToExe(cppFile);
        JudgeResult result = Judger.judge(exeFile, input, expected_output, 2000, MEMORY_LIMIT_KB, CompareMode.STRICT, true);
        assertEquals(Judger.ACCEPTED, result.statusCode);
        assertTrue(result.time >= 0);}

    //compile error
    @Test
    public void testCompileError() throws IOException, MemoryLimitExceededException {
        String shitcode = "#include<iostream>\nint main(){fuckyou}";
        File cppFile = createTempCppFile(shitcode);
        File exeFile = compileCppToExe(cppFile);
        if (!exeFile.exists()) {
            JudgeResult result = new JudgeResult(Judger.COMPILE_ERROR, 0.0, 0L);
            assertEquals(Judger.COMPILE_ERROR, result.statusCode);
        } else {
            JudgeResult result = Judger.judge(exeFile, input, expected_output, 2000, MEMORY_LIMIT_KB, CompareMode.STRICT, true);
            assertEquals(Judger.COMPILE_ERROR, result.statusCode);
        }
    }

    //wrong answer
    @Test
    public void testWrongAnswer() throws IOException, MemoryLimitExceededException {
        String ssshit = "#include<iostream>\nint main(){std::cout<<0721<<std::endl;return 0;}";
        File cppFile = createTempCppFile(ssshit);
        File exeFile = compileCppToExe(cppFile);
        JudgeResult result = Judger.judge(exeFile, input, expected_output, 2000, MEMORY_LIMIT_KB, CompareMode.STRICT, true);
        assertEquals(Judger.WRONG_ANSWER, result.statusCode);
    }

    //real time limit exceeded
    @Test
    public void testTimeLimitExceeded() throws IOException, MemoryLimitExceededException {
        String ciallo = "#include<unistd.h>\nint main(){sleep(3);return 0;}";
        File cppFile = createTempCppFile(ciallo);
        File exeFile = compileCppToExe(cppFile);
        JudgeResult result = Judger.judge(exeFile, "", "", 1000, MEMORY_LIMIT_KB, CompareMode.STRICT, true);
        assertEquals(Judger.REAL_TIME_LIMIT_EXCEEDED, result.statusCode);
    }


    private File createTempCppFile(String source) throws IOException {
        File tempFile = File.createTempFile("test", ".cpp");
        try (FileWriter writer = new FileWriter(tempFile)) { writer.write(source); }
        tempFile.deleteOnExit();
        return tempFile;
    }

    @SneakyThrows
    private File compileCppToExe(File cppFile) {
        File tempDir = Files.createTempDirectory("test_compile").toFile();
        String exeName = java.util.UUID.randomUUID().toString().replace("-", "");
        if (System.getProperty("os.name").toLowerCase().contains("win")) exeName += ".exe";
        File exeFile = new File(tempDir, exeName);
        int compileCode = Compiler.compileProgram(cppFile, exeFile, false, false, 5000, "g++", "c++11");
        exeFile.deleteOnExit();
        tempDir.deleteOnExit();
        return exeFile;
    }
}