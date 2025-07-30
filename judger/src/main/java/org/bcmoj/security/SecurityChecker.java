package org.bcmoj.security;

import java.io.File;

public interface SecurityChecker {
    int check(File codeFile, File keywordsFile);
}
