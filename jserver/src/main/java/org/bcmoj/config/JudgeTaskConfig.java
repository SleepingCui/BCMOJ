package org.bcmoj.config;
import lombok.Getter;

import java.io.File;

@Getter
public class JudgeTaskConfig {
    private final String compilerPath;
    private final String cppStandard;
    private final File sourceFile; // The uploaded .cpp file
    private final File keywordFile; // The rules file
    private final boolean disableSecurityArgs;
    private final boolean disableMemLimit;
    private final boolean useOldFormat;

    private JudgeTaskConfig(Builder builder) {
        this.compilerPath = builder.compilerPath;
        this.cppStandard = builder.cppStandard;
        this.sourceFile = builder.sourceFile;
        this.keywordFile = builder.keywordFile;
        this.disableSecurityArgs = builder.disableSecurityArgs;
        this.disableMemLimit = builder.disableMemLimit;
        this.useOldFormat = builder.useOldFormat;
    }

    public static class Builder {
        private String compilerPath = "g++"; // Default
        private String cppStandard = "c++11"; // Default
        private File sourceFile;
        private File keywordFile;
        private boolean disableSecurityArgs = false;
        private boolean disableMemLimit = false;
        private boolean useOldFormat = false;

        public Builder compilerPath(String compilerPath) {
            this.compilerPath = compilerPath;
            return this;
        }

        public Builder cppStandard(String cppStandard) {
            this.cppStandard = cppStandard;
            return this;
        }

        public Builder sourceFile(File sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder keywordFile(File keywordFile) {
            this.keywordFile = keywordFile;
            return this;
        }

        public Builder disableSecurityArgs(boolean disableSecurityArgs) {
            this.disableSecurityArgs = disableSecurityArgs;
            return this;
        }

        public Builder disableMemLimit(boolean disableMemLimit) {
            this.disableMemLimit = disableMemLimit;
            return this;
        }

        public Builder useOldFormat(boolean useOldFormat) {
            this.useOldFormat = useOldFormat;
            return this;
        }

        public JudgeTaskConfig build() {
            if (sourceFile == null || keywordFile == null) {
                throw new IllegalStateException("Source file and Keyword file are required.");
            }
            return new JudgeTaskConfig(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}