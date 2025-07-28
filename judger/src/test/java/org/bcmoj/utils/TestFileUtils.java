package org.bcmoj.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TestFileUtils {
    public static File createTempCppFile(String cppContent) throws IOException {
        File tmp = File.createTempFile("temp_source_", ".cpp");
        tmp.deleteOnExit();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(tmp))) {
            w.write(cppContent);
        }
        return tmp;
    }
    public static File createTempKeywordFile(List<String> keywords) throws IOException {
        File tmp = File.createTempFile("temp_keywords_", ".txt");
        tmp.deleteOnExit();
        KWFileWriter.writeKeywords(tmp.getAbsolutePath(), keywords);
        return tmp;
    }
    public static File createTempDefaultKeywordFile() throws IOException {
        File tmp = File.createTempFile("temp_default_keywords_", ".txt");
        tmp.deleteOnExit();
        KWFileWriter.writeDefaultKeywords(tmp.getAbsolutePath());
        return tmp;
    }
}
