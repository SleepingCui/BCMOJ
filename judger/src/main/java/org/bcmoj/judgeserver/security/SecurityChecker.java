package org.bcmoj.judgeserver.security;

import java.io.File;

public interface SecurityChecker {
    int check(File codeFile, File keywordsFile);
}
