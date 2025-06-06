package org.bcmoj;

import org.bcmoj.config.ConfigProcess;
import org.bcmoj.netserver.JCFSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * BCMOJ Judge Server
 * <p>
 *     Judge Server Main Program
 * </p>
 * @author SleepingCui
 * @since 2025
 * @version 1.0-SNAPSHOT
 * @see <a href="https://github.com/SleepingCui/BCMOJ">Github Repository</a>
 */

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logo();
        parseArguments(args);
    }

    private static void parseArguments(String[] args) {
        Properties cmdProps = new Properties();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    cmdProps.setProperty(parts[0], parts[1]);
                }
            }
        }
        if (cmdProps.containsKey("config")) {
            try (FileInputStream fis = new FileInputStream(cmdProps.getProperty("config"))) {
                Properties fileProps = new Properties();
                fileProps.load(fis);
                fileProps.forEach((k, v) -> cmdProps.putIfAbsent(k.toString(), v.toString()));
            } catch (IOException e) {
                logger.error("Failed to load config file: {}", e.getMessage());
                System.exit(1);
            }
        }

        startServer(cmdProps);
    }
    private static void startServer(Properties props) {
        String portStr = props.getProperty("port", ConfigProcess.GetConfig("ServerPort"));
        String ip = props.getProperty("host", ConfigProcess.GetConfig("ServerIP"));
        String kwFile = props.getProperty("kwfile", ConfigProcess.GetConfig("KeywordsFile"));

        if (isBlank(portStr) || isBlank(ip)) {
            logger.error("Configuration missing: Port={}, IP={}", portStr, ip);
            System.exit(1);
        }
        if (!isBlank(kwFile)) {
            ConfigProcess.UpdateConfig("KeywordsFile", kwFile);
        }

        try {
            JCFSocketServer server = new JCFSocketServer();
            server.start(Integer.parseInt(portStr), ip, kwFile);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Server shutting down.");
                server.stop();
            }));
            logger.info("Starting server at {}:{} with keywords file: {}", ip, portStr, kwFile);
        } catch (NumberFormatException e) {
            logger.error("Invalid port: {}", portStr);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Startup failed: {}", e.toString());
            System.exit(1);
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static void logo() {
        String logo = """
                  ____   ____ __  __  ___      _       _ ____                          \s
                 | __ ) / ___|  \\/  |/ _ \\    | |     | / ___|  ___ _ ____   _____ _ __\s
                 |  _ \\| |   | |\\/| | | | |_  | |  _  | \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|
                 | |_) | |___| |  | | |_| | |_| | | |_| |___) |  __/ |   \\ V /  __/ |  \s
                 |____/ \\____|_|  |_|\\___/ \\___/   \\___/|____/ \\___|_|    \\_/ \\___|_|  \s
                
                    BCMOJ Judge Server v1.0-SNAPSHOT  Developed by SleepingCui
                """;
        System.out.println(logo);
    }
}