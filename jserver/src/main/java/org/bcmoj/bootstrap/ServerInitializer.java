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
 * Utility class responsible for initializing and starting the BCMOJ Judge Server.
 * <p>
 * This class handles loading required properties from command-line or config file,
 * setting up logging, initializing the keyword file if it does not exist, and
 * starting the {@link SocketServer} with the appropriate configuration.
 * </p>
 */
@Slf4j
public class ServerInitializer {

    /**
     * Starts the BCMOJ server with the given properties.
     * <p>
     * Loads required server configuration, initializes the keyword file,
     * and starts the SocketServer with optional compiler path.
     * </p>
     *
     * @param props          Merged Properties object containing server configuration
     * @param configFilePath Optional path to configuration file
     * @param debug          Whether to enable debug logging
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
        startServer(config.host, config.port, config.kwFile, config.compilerPath, config.cppStandard);
    }
    private static void configureLogging(boolean debug) {
        if (debug) {
            Configurator.setRootLevel(Level.DEBUG);
            log.debug("Debug enabled");
        } else {
            Configurator.setRootLevel(Level.INFO);
        }
    }

    /**
     * Loads required server properties from the provided Properties object
     * or from the configuration file if any are missing.
     *
     * @param props          Properties object with initial values
     * @param configFilePath Optional path to configuration file
     * @return ServerConfig containing host, port, keyword file, and compiler path
     * @throws IllegalArgumentException if required properties are missing or invalid
     */
    private static ServerConfig loadRequiredProperties(Properties props, String configFilePath) {
        String host = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");
        String compilerPath = props.getProperty("CompilerPath");
        String cppStandard = props.getProperty("CppStandard");

        // Load missing properties from config file
        if (configFilePath != null && (isBlank(host) || isBlank(portStr) || isBlank(kwFile) || isBlank(compilerPath) || isBlank(cppStandard))) {
            Properties fileProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                fileProps.load(fis);
                if (isBlank(host)) host = fileProps.getProperty("ServerIP");
                if (isBlank(portStr)) portStr = fileProps.getProperty("ServerPort");
                if (isBlank(kwFile)) kwFile = fileProps.getProperty("KeywordsFile");
                if (isBlank(compilerPath)) compilerPath = fileProps.getProperty("CompilerPath");
                if (isBlank(cppStandard)) cppStandard = fileProps.getProperty("CppStandard");
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
        // defaults
        if (isBlank(compilerPath)) compilerPath = "g++";
        if (isBlank(cppStandard)) cppStandard = "c++11";

        return new ServerConfig(host, port, kwFile, compilerPath, cppStandard);
    }

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
     * Starts the SocketServer with the given host, port, keyword file, and compiler path.
     * <p>
     * Adds a shutdown hook to stop the server gracefully on JVM exit.
     * </p>
     *
     * @param host         server host
     * @param port         server port
     * @param kwFile       path to keyword file
     * @param compilerPath path to C++ compiler (default "g++" if not provided)
     * @param cppStandard  C++ standard version
     */
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

    /**
     * Simple container for validated server configuration.
     */
    private static class ServerConfig {
        final String host;
        final int port;
        final String kwFile;
        final String compilerPath;
        final String cppStandard;

        ServerConfig(String host, int port, String kwFile, String compilerPath, String cppStandard) {
            this.host = host;
            this.port = port;
            this.kwFile = kwFile;
            this.compilerPath = compilerPath;
            this.cppStandard = cppStandard;
        }
    }
}
