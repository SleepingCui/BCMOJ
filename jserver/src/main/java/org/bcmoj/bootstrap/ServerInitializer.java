package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bcmoj.netserver.SocketServer;
import org.bcmoj.utils.ComplierCheckUtil;
import org.bcmoj.utils.KeywordFileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.List;

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
        boolean disableSecArgs = cmd.hasOption("disable_security_args");

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
        startServer(host, port, kwFile, disableSecArgs ,compilerPath, cppStandard);
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

    /**
     * Checks the g++ version and determines whether to disable compiler security flags.
     *
     * @param DisableSecurityArgs current value of DisableSecurityArgs
     * @param compilerPath        path to g++ executable
     * @return updated DisableSecurityArgs value
     */
    private static boolean checkAndHandleCompilerSecurity(boolean DisableSecurityArgs, String compilerPath) {
        if (!DisableSecurityArgs) {
            String gppVersion = ComplierCheckUtil.getGppVersion(compilerPath);
            if (gppVersion != null) {
                String[] parts = gppVersion.split("\\.");
                int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

                if (major < 4 || (major == 4 && minor < 9)) {
                    log.warn("********************************************************************************");
                    log.warn("                                   WARNING");
                    log.warn("          Detected g++ version {} is older than 4.9.0", gppVersion);
                    log.warn("      Compiler security flags will be automatically disabled for safety.");
                    log.warn("********************************************************************************");
                    return true;
                }
            } else {
                log.warn("Failed to detect g++ version, disabling security flags as precaution.");
                return true;
            }
        } else {
            log.warn("********************************************************************************");
            log.warn("                                   WARNING");
            log.warn(" Compiler security flags will be disabled, which may reduce compilation safety.");
            log.warn("********************************************************************************");
        }
        return DisableSecurityArgs;
    }


    private static void startServer(String host, int port, String kwFile, boolean DisableSecurityArgs, String compilerPath, String cppStandard) {
        try {
            DisableSecurityArgs = checkAndHandleCompilerSecurity(DisableSecurityArgs, compilerPath);

            log.info("Starting server...");
            SocketServer server = new SocketServer(host, port, DisableSecurityArgs, kwFile, compilerPath, cppStandard);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        } catch (Exception e) {
            log.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }

}
