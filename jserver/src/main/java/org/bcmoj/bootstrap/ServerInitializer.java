package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bcmoj.netserver.SocketServer;
import org.bcmoj.utils.KeywordFileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Initializes and starts the BCMOJ server.
 * Handles configuration loading, logging setup, keyword file initialization, and server startup.
 *
 * @author BCMOJ
 * @version ${project.version}
 */
@Slf4j
public class ServerInitializer {

    /**
     * Starts the BCMOJ server with the given properties and configuration file.
     *
     * @param props         Properties object containing server configuration
     * @param configFilePath Path to additional configuration file (optional)
     * @param debug         Whether to enable debug logging
     */
    public static void start(Properties props, String configFilePath, boolean debug) {
        configureLogging(debug);

        ServerConfig config;
        try {
            config = loadRequiredProperties(props, configFilePath);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            Bootstrap.printHelp();
            System.exit(1);
            return;
        }

        initKeywordFile(config.kwFile);
        startServer(config.host, config.port, config.kwFile);
    }

    /**
     * Configures logging level based on debug flag.
     *
     * @param debug whether to enable debug logging
     */
    private static void configureLogging(boolean debug) {
        if (debug) {
            Configurator.setRootLevel(Level.DEBUG);
            log.debug("Debug enabled");
        } else {
            Configurator.setRootLevel(Level.INFO);
        }
    }

    /**
     * Loads required server properties from the provided Properties object or a configuration file.
     *
     * @param props           initial Properties object
     * @param configFilePath  optional path to configuration file
     * @return ServerConfig containing validated host, port, and keyword file path
     * @throws IllegalArgumentException if any required property is missing or invalid
     */
    private static ServerConfig loadRequiredProperties(Properties props, String configFilePath) {
        String host = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");

        // Load missing properties from config file if needed
        if (configFilePath != null && (isBlank(host) || isBlank(portStr) || isBlank(kwFile))) {
            Properties fileProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                fileProps.load(fis);
                if (isBlank(host)) host = fileProps.getProperty("ServerIP");
                if (isBlank(portStr)) portStr = fileProps.getProperty("ServerPort");
                if (isBlank(kwFile)) kwFile = fileProps.getProperty("KeywordsFile");
                log.info("Loaded missing parameters from config file: {}", configFilePath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to load config file '" + configFilePath + "': " + e.getMessage());
            }
        }

        if (isBlank(host) || isBlank(portStr) || isBlank(kwFile)) {
            throw new IllegalArgumentException(String.format(
                    "Missing required parameters: host=%s, port=%s, kwfile=%s", host, portStr, kwFile));
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number: " + portStr);
        }

        return new ServerConfig(host, port, kwFile);
    }

    /**
     * Initializes the keyword file if it does not exist.
     *
     * @param kwFile path to keyword file
     */
    private static void initKeywordFile(String kwFile) {
        try {
            if (KeywordFileUtil.createDefaultIfNotExists(kwFile)) {
                log.info("Keyword file not found, created default keyword file at: {}", kwFile);
            }
        } catch (IOException e) {
            log.warn("Could not create keyword file '{}': {}", kwFile, e.getMessage());
        }
    }

    /**
     * Starts the SocketServer and adds a shutdown hook.
     *
     * @param host   server host
     * @param port   server port
     * @param kwFile path to keyword file
     */
    private static void startServer(String host, int port, String kwFile) {
        try {
            log.info("Starting server...");
            SocketServer server = new SocketServer(host, port, kwFile);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            log.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Checks if a string is null or blank.
     *
     * @param s input string
     * @return true if null or blank, false otherwise
     */
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Simple container class for validated server configuration.
     */
    private static class ServerConfig {
        final String host;
        final int port;
        final String kwFile;

        ServerConfig(String host, int port, String kwFile) {
            this.host = host;
            this.port = port;
            this.kwFile = kwFile;
        }
    }
}
