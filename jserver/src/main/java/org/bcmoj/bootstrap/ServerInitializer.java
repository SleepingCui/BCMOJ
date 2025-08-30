package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bcmoj.netserver.SocketServer;
import org.bcmoj.utils.KeywordFileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This class is responsible for initializing and starting the BCMOJ server.
 * It handles command line arguments, optional configuration file loading,
 * logging configuration, keyword file initialization, and server startup.
 */
@Slf4j
public class ServerInitializer {
    public static void start(CommandLine cmd) {
        boolean debug = cmd.hasOption("debug");
        configureLogging(debug);

        Properties props = new Properties();
        String configFilePath = cmd.getOptionValue("config");
        if (configFilePath != null) {
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                props.load(fis);
                log.info("Loaded config file: {}", configFilePath);
            } catch (IOException e) {
                log.error("Failed to load config file '{}': {}", configFilePath, e.getMessage());
                System.exit(1);
            }
        }

        if (cmd.hasOption("host")) props.setProperty("host", cmd.getOptionValue("host"));
        if (cmd.hasOption("port")) props.setProperty("port", cmd.getOptionValue("port"));
        if (cmd.hasOption("kwfile")) props.setProperty("kwfile", cmd.getOptionValue("kwfile"));
        if (cmd.hasOption("comp")) props.setProperty("CompilerPath", cmd.getOptionValue("comp"));
        if (cmd.hasOption("std")) props.setProperty("CppStandard", cmd.getOptionValue("std"));

        // default values
        props.putIfAbsent("CompilerPath", "g++");
        props.putIfAbsent("CppStandard", "c++11");

        String host = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");
        String compilerPath = props.getProperty("CompilerPath");
        String cppStandard = props.getProperty("CppStandard");

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

        if (isBlank(host) || isBlank(portStr) || isBlank(kwFile)) {
            log.error("Missing required parameters: host={}, port={}, kwfile={}", host, portStr, kwFile);
            System.exit(1);
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
