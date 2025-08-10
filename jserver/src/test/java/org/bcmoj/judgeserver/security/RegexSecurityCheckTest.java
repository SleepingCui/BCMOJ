package org.bcmoj.judgeserver.security;

import org.bcmoj.security.RegexSecurityCheck;
import org.bcmoj.testutils.TestFileUtils;
import org.junit.*;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class RegexSecurityCheckTest {

    private RegexSecurityCheck checker;

    @Before
    public void setUp() {
        checker = new RegexSecurityCheck();
    }

    //system
    @Test
    public void testDangerousSystemCall() throws Exception {
        File cppFile = TestFileUtils.createTempCppFile("int main() { system(\"ls\"); return 0; }");
        File kwFile = TestFileUtils.createTempDefaultKeywordFile();

        int result = checker.check(cppFile, kwFile);
        assertEquals(-5, result);
    }
    //pass
    @Test
    public void testSafeCodeWithRegexRule() throws Exception {
        File cppFile = TestFileUtils.createTempCppFile("int main() { return 0; }");
        File kwFile = TestFileUtils.createTempKeywordFile(List.of("regex:System\\.exit"));

        int result = checker.check(cppFile, kwFile);
        assertEquals(0, result);
    }
    //system
    @Test
    public void testCodeWithRegexMatch() throws Exception {
        File cppFile = TestFileUtils.createTempCppFile("int main() { System.exit(1); return 0; }");
        File kwFile = TestFileUtils.createTempKeywordFile(List.of("regex:System\\.exit"));

        int result = checker.check(cppFile, kwFile);
        assertEquals(-5, result);
    }
}
