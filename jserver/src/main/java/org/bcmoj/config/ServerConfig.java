package org.bcmoj.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration object for the BCMOJ Judge Server.
 * Contains all settings needed for initialization and startup.
 * Uses the Builder pattern for construction.
 */
@Slf4j
@Getter
public class ServerConfig {
    private final String host;
    private final int port;
    private final String keywordFilePath;
    private final String compilerPath;
    private final String cppStandard;
    private final int nettyThreads;
    private final boolean disableSecurityArgs;
    private final boolean disableMemLimit;
    private final boolean useOldFormat;

    private ServerConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.keywordFilePath = builder.keywordFilePath;
        this.compilerPath = builder.compilerPath;
        this.cppStandard = builder.cppStandard;
        this.nettyThreads = builder.nettyThreads;
        this.disableSecurityArgs = builder.disableSecurityArgs;
        this.disableMemLimit = builder.disableMemLimit;
        this.useOldFormat = builder.useOldFormat;
    }

    /**
     * Builder class for ServerConfiguration.
     */
    public static class Builder {
        private String host;
        private int port;
        private String keywordFilePath;
        private String compilerPath = "g++";
        private String cppStandard = "c++11";
        private int nettyThreads = 1;
        private boolean disableSecurityArgs = false;
        private boolean disableMemLimit = false;
        private boolean useOldFormat = false;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder keywordFilePath(String keywordFilePath) {
            this.keywordFilePath = keywordFilePath;
            return this;
        }

        public Builder compilerPath(String compilerPath) {
            this.compilerPath = compilerPath;
            return this;
        }

        public Builder cppStandard(String cppStandard) {
            this.cppStandard = cppStandard;
            return this;
        }

        public Builder nettyThreads(int nettyThreads) {
            if (nettyThreads < 1) {
                nettyThreads = 1;
                log.warn("Invalid netty-threads, must be >=1. Using default 1.");
            }
            this.nettyThreads = nettyThreads;
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

        public ServerConfig build() {
            if (this.host == null || this.port <= 0 || this.keywordFilePath == null) {
                throw new IllegalStateException("Host, Port, and KeywordFilePath are required.");
            }
            return new ServerConfig(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
