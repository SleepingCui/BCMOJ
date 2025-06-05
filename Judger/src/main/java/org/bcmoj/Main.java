package org.bcmoj;

import org.bcmoj.config.ConfigProcess;
import org.bcmoj.netserver.JCFSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        on_start(args);
    }


    private static String getConfigValue(String[] args, int index, String configKey) {
        return (args.length > index && !args[index].isBlank()) ? args[index] : ConfigProcess.GetConfig(configKey);
    }
    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
    private static void on_start(String[] args) {
        String portStr = getConfigValue(args, 0, "ServerPort");
        String ip = getConfigValue(args, 1, "ServerIP");

        if (isBlank(portStr) || isBlank(ip)) {
            logger.error("Configuration missing: Port={}, IP={}", portStr, ip);
            System.exit(1);
        }
        try {
            JCFSocketServer server = new JCFSocketServer();
            server.start(Integer.parseInt(portStr), ip);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Server shutting down.");
                server.stop();
            }));
            logger.info("Starting server at {}:{}", ip, portStr);
        } catch (NumberFormatException e) {
            logger.error("Invalid port: {}", portStr);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Startup failed: {}", e.toString());
            System.exit(1);
        }
    }

    public static void logo(){
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