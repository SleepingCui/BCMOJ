package org.bcmoj.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class OutputCompareUtilTest {

    @Test
    public void testStrictMatch() {
        assertTrue(OutputCompareUtil.compare("hello", "hello", OutputCompareUtil.CompareMode.STRICT));
        assertFalse(OutputCompareUtil.compare("hello", "Hello", OutputCompareUtil.CompareMode.STRICT));
        assertFalse(OutputCompareUtil.compare("hello ", "hello", OutputCompareUtil.CompareMode.STRICT));
        assertFalse(OutputCompareUtil.compare(null, "hello", OutputCompareUtil.CompareMode.STRICT));
        assertFalse(OutputCompareUtil.compare("hello", null, OutputCompareUtil.CompareMode.STRICT));
    }

    @Test
    public void testIgnoreSpaces() {
        assertTrue(OutputCompareUtil.compare("hello world", "hello   world", OutputCompareUtil.CompareMode.IGNORE_SPACES));
        assertTrue(OutputCompareUtil.compare("  hello \n world ", "hello world", OutputCompareUtil.CompareMode.IGNORE_SPACES));
        assertFalse(OutputCompareUtil.compare("hello world", "helloworld", OutputCompareUtil.CompareMode.IGNORE_SPACES));
    }

    @Test
    public void testCaseInsensitive() {
        assertTrue(OutputCompareUtil.compare("Hello World", "hello world", OutputCompareUtil.CompareMode.CASE_INSENSITIVE));
        assertFalse(OutputCompareUtil.compare("Hello World!", "hello world", OutputCompareUtil.CompareMode.CASE_INSENSITIVE));
    }

    @Test
    public void testFloatTolerant() {
        assertTrue(OutputCompareUtil.compare("3.1415926 2.71828", "3.1415927 2.718281", OutputCompareUtil.CompareMode.FLOAT_TOLERANT));
        assertFalse(OutputCompareUtil.compare("3.14 2.7", "3.14", OutputCompareUtil.CompareMode.FLOAT_TOLERANT));
        assertFalse(OutputCompareUtil.compare("3.14 hello", "3.14 world", OutputCompareUtil.CompareMode.FLOAT_TOLERANT));
        assertFalse(OutputCompareUtil.compare("1.000001", "1.00001", OutputCompareUtil.CompareMode.FLOAT_TOLERANT));
    }

    @Test
    public void testDefaultCompare() {
        assertTrue(OutputCompareUtil.compare("exact match", "exact match"));
        assertFalse(OutputCompareUtil.compare("exact match", "Exact Match"));
    }
}
