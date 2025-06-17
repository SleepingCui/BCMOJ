package org.bcmoj;

import org.bcmoj.utils.ConfigProcess;
import org.bcmoj.netserver.JCFSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * BCMOJ Judge Server
 * Judge Server Main Program
 *
 * @author SleepingCui & MxingFoew1034
 * @since 2025
 * @version 1.0-SNAPSHOT
 */

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logo();
        if (args.length == 0) {
            showHelp();
            System.exit(0);
        }
        parseArguments(args);
    }

    private static void showHelp() {
        System.out.println("""
            BCMOJ Judge Server - Usage:

            Required (either via command-line or config file):
              --host=<IP>        Server IP address (e.g., 0.0.0.0)
              --port=<PORT>      Port number (e.g., 5000)
              --kwfile=<FILE>    Keyword file path

            Optional:
              --config=<FILE>    External config.properties path

            Examples:
              java -jar code.jar --host=0.0.0.0 --port=5000 --kwfile=keywords.txt
              java -jar code.jar --config=config.properties
              java -jar code.jar --config=config.properties --host=192.168.1.1
            
            Note: Command line parameters have higher priority than config file values.
            """);
    }

    private static void parseArguments(String[] args) {
        Properties finalProps = new Properties();
        String configFilePath = null;
        Properties cmdLineProps = new Properties();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    if (parts[0].equals("config")) {
                        configFilePath = parts[1];
                    } else {
                        cmdLineProps.setProperty(parts[0], parts[1]);
                    }
                }
            }
        }
        if (configFilePath != null) {
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                Properties fileProps = new Properties();
                fileProps.load(fis);
                fileProps.forEach((k, v) -> finalProps.setProperty(k.toString(), v.toString()));

                logger.info("Loaded config file: {}", configFilePath);
            } catch (IOException e) {
                logger.error("Error loading config file '{}': {}", configFilePath, e.getMessage());
                System.exit(1);
            }
        }
        cmdLineProps.forEach((k, v) -> finalProps.setProperty(k.toString(), v.toString()));

        startServer(finalProps, configFilePath != null);
    }

    private static void startServer(Properties props, boolean hasConfigFile) {
        String ip = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");
        boolean needDefaultConfig = isBlank(ip) || isBlank(portStr) || isBlank(kwFile);


        if (needDefaultConfig && !hasConfigFile) {
            logger.info("Loading default configuration for missing parameters...");

            if (isBlank(ip)) {
                ip = ConfigProcess.GetConfig("ServerIP");
            }
            if (isBlank(portStr)) {
                portStr = ConfigProcess.GetConfig("ServerPort");
            }
            if (isBlank(kwFile)) {
                kwFile = ConfigProcess.GetConfig("KeywordsFile");
            }
        }
        if (isBlank(ip) || isBlank(portStr)) {
            logger.error("Missing required parameters: host={}, port={}", ip, portStr);
            showHelp();
            System.exit(1);
        }
        if (isBlank(kwFile)) {
            kwFile = "keywords.txt";
            logger.info("Using default keyword file: {}", kwFile);
            try {
                org.bcmoj.utils.KWFileWriter.createDefaultIfNotExists(kwFile);
            } catch (IOException e) {
                logger.warn("Could not create keyword file: {}", e.getMessage());
            }
        }
        try {
            int port = Integer.parseInt(portStr);
            JCFSocketServer server = new JCFSocketServer();

            server.start(port, ip, kwFile);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Server shutting down gracefully...");
                server.stop();
            }));
            logger.info("BCMOJ Judge Server started successfully at {}:{}  keyword file: {}", ip, port, kwFile);

        } catch (NumberFormatException e) {
            logger.error("Invalid port number: {}", portStr);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static void logo() {
        String logo = """
                  ____   ____ __  __  ___      _       _ ____                        \s
                 | __ ) / ___|  \\/  |/ _ \\    | |     | / ___|  ___ _ ____   _____ _ __
                 |  _ \\| |   | |\\/| | | | |_  | |  _  | \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|
                 | |_) | |___| |  | | |_| | |_| | | |_| |___) |  __/ |   \\ V /  __/ |  \s
                 |____/ \\____|_|  |_|\\___/ \\___/   \\___/|____/ \\___|_|    \\_/ \\___|_|  \s

                BCMOJ Judge Server v1.0-SNAPSHOT  Developed by SleepingCui & MxingFoew1034
               \s""";
        System.out.println(logo);
    }
}