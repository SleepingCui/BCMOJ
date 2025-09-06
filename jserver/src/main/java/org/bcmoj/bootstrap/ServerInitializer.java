package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bcmoj.netserver.SocketServer;
import org.bcmoj.utils.KeywordFileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * Initializes and starts the BCMOJ Judge Server.
 * Handles command line arguments, config file, logging, keyword file, and server startup.
 */
@Slf4j
public class ServerInitializer {

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
        if ((host == null || portStr == null || kwFile == null) && configFilePath == null) {
            log.error("Missing required parameters: host={}, port={}, kwfile={}", host, portStr, kwFile);
            CLIParser.printHelp();
            System.exit(1);
        }
        if (debug) {
            log.debug("--------------------------------");
            log.debug("Host: {}", host);
            log.debug("Port: {}", portStr);
            log.debug("Keyword file: {}", kwFile);
            log.debug("Compiler path: {}", compilerPath.equals("g++") ? compilerPath + " (default value)" : compilerPath);
            log.debug("C++ standard: {}", cppStandard.equals("c++11") ? cppStandard + " (default value)" : cppStandard);
            log.debug("Config file: {}", configFilePath != null ? configFilePath : "none");
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
                org.bcmoj.utils.PropertiesExportUtil.export(props);
            } catch (Exception e) {
                log.error("Failed to export properties: {}", e.getMessage());
            }
            return;
        }
        startServer(host, port, kwFile, compilerPath, cppStandard);
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

    private static void startServer(String host, int port, String kwFile, String compilerPath, String cppStandard) {
        try {
            log.info("Starting server...");
            SocketServer server = new SocketServer(host, port, kwFile, compilerPath, cppStandard);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            log.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }
}
