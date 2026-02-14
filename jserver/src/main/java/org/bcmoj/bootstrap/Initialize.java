package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bcmoj.config.ServerConfig;
import org.bcmoj.utils.ComplierCheckUtil;
import org.bcmoj.utils.KeywordFileUtil;
import org.bcmoj.utils.PropertiesExportUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.List;

/**
 * Initializes the BCMOJ Judge Server environment.
 * Handles command line arguments, config file loading, logging setup,
 * keyword file preparation, and constructs the final ServerConfig object.
 * Delegates server startup to ServerLauncher.
 */
@Slf4j
public class Initialize {

    public static void start(CommandLine cmd) {
        boolean debug = cmd.hasOption("debug");
        configureLogging(debug);
        Properties props = CLIParser.toProperties(cmd);
        String configFilePath = props.getProperty("config");
        if (configFilePath != null) {
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                Properties fileProps = new Properties();
                fileProps.load(fis);
                fileProps.forEach(props::putIfAbsent);
                log.info("Loaded config file: {}", configFilePath);
            } catch (IOException e) {
                log.error("Failed to load config file '{}': {}", configFilePath, e.getMessage());
                System.exit(1);
            }
        }

        String host = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");
        String compilerPath = props.getProperty("CompilerPath", "g++");
        String cppStandard = props.getProperty("CppStandard", "c++11");
        String nettyThreadsStr = props.getProperty("netty-threads");
        boolean disableSecArgs = cmd.hasOption("disable-security-args");
        boolean disableMemLimit = cmd.hasOption("disable-mem-limit");
        boolean useOldFormat = cmd.hasOption("use-old-format");
        int nettyThreads = 1;
        if (nettyThreadsStr != null) {
            try {
                nettyThreads = Integer.parseInt(nettyThreadsStr);
                if (nettyThreads < 1) {
                    log.warn("Invalid netty-threads '{}', must be >=1. Using default 1.", nettyThreadsStr);
                    nettyThreads = 1;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid netty-threads '{}', using default 1.", nettyThreadsStr);
            }
        }

        if ((host == null || portStr == null || kwFile == null) && configFilePath == null) {
            List<String> missing = new ArrayList<>();
            if (host == null) missing.add("host");
            if (portStr == null) missing.add("port");
            if (kwFile == null) missing.add("kwfile");

            log.error("Missing required parameters: {}", String.join(", ", missing));
            CLIParser.printHelp();
            System.exit(1);
        }
        if (debug) {
            log.debug("--------------------------------");
            log.debug("Compiler Version: {}",ComplierCheckUtil.getGppVersion(compilerPath));
            log.debug("Host: {}", host);
            log.debug("Port: {}", portStr);
            log.debug("Keyword file: {}", kwFile);
            log.debug("Compiler path: {}", compilerPath.equals("g++") ? compilerPath + " (default value)" : compilerPath);
            log.debug("C++ standard: {}", cppStandard.equals("c++11") ? cppStandard + " (default value)" : cppStandard);
            log.debug("Config file: {}", configFilePath != null ? configFilePath : "none");
            log.debug("Netty threads: {}{}", nettyThreads, nettyThreads == 1 ? " (default value)" : "");
            log.debug("--------------------------------");
        }

        int port;
        try {
            port = Integer.parseInt(Objects.requireNonNull(portStr));
        } catch (NumberFormatException e) {
            log.error("Invalid port number: {}", portStr);
            System.exit(1);
            return;
        }
        initKeywordFile(kwFile);
        if (cmd.hasOption("extract")) {
            try {
                PropertiesExportUtil.export(props);
                System.exit(0);
            } catch (Exception e) {
                log.error("Failed to export properties: {}", e.getMessage());
            }
            return;
        }
        ServerConfig config = ServerConfig.builder().host(host).port(port).keywordFilePath(kwFile).compilerPath(compilerPath).cppStandard(cppStandard).nettyThreads(nettyThreads).disableSecurityArgs(disableSecArgs).disableMemLimit(disableMemLimit).useOldFormat(useOldFormat).build();
        ServerLauncher.launch(config);
    }

    private static void configureLogging(boolean debug) {
        if (debug) {
            Configurator.setRootLevel(Level.DEBUG);
            log.debug("Debug logging enabled");
        } else {
            Configurator.setRootLevel(Level.INFO);
        }
    }

    private static void initKeywordFile(String kwFile) {
        try {
            if (KeywordFileUtil.createDefaultIfNotExists(kwFile)) {
                log.info("Keyword file not found, created default at: {}", kwFile);
            }
        } catch (IOException e) {
            log.warn("Could not create keyword file '{}': {}", kwFile, e.getMessage());
        }
    }

    private static ServerConfig applySecurityArgLogic(ServerConfig config) {
        if (!config.isDisableSecurityArgs()) {
            String gppVersion = ComplierCheckUtil.getGppVersion(config.getCompilerPath());
            if (gppVersion != null) {
                String[] parts = gppVersion.split("\\.");
                int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

                if (major < 4 || (major == 4 && minor < 9)) {
                    log.warn("Detected g++ version {} is older than 4.9.0. \nCompiler security flags will be automatically disabled.", gppVersion);
                    boolean newDisableFlag = true;
                    return ServerConfig.builder().host(config.getHost()).port(config.getPort()).keywordFilePath(config.getKeywordFilePath()).compilerPath(config.getCompilerPath()).cppStandard(config.getCppStandard()).nettyThreads(config.getNettyThreads()).disableSecurityArgs(newDisableFlag).disableMemLimit(config.isDisableMemLimit()).useOldFormat(config.isUseOldFormat()).build();
                }
            } else {
                log.warn("Failed to detect g++ version, disabling security flags as precaution.");
                boolean newDisableFlag = true;
                return ServerConfig.builder().host(config.getHost()).port(config.getPort()).keywordFilePath(config.getKeywordFilePath()).compilerPath(config.getCompilerPath()).cppStandard(config.getCppStandard()).nettyThreads(config.getNettyThreads()).disableSecurityArgs(newDisableFlag).disableMemLimit(config.isDisableMemLimit()).useOldFormat(config.isUseOldFormat()).build();
            }
        }
        return config;
    }
    public static ServerConfig processConfig(ServerConfig initialConfig) {
        return applySecurityArgLogic(initialConfig);
    }
}