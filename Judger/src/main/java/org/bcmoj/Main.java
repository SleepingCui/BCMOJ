package org.bcmoj;

import org.bcmoj.config.ConfigProcess;
import org.bcmoj.netserver.JCFSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        logo();
        on_start(args);
    }

    private static void on_start(String[] args) {
        String portStr = (args.length > 0 && !args[0].isBlank()) ? args[0] : ConfigProcess.GetConfig("ServerPort");
        String ip = (args.length > 1 && !args[1].isBlank()) ? args[1] : ConfigProcess.GetConfig("ServerIP");

        if (portStr == null || portStr.isBlank()) {
            logger.error("Port not specified: {}",portStr);
            System.exit(1);
        }
        if (ip == null || ip.isBlank()) {
            logger.error("IP address not specified: {}",ip);
            System.exit(1);
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            logger.error("Invalid port: {}", portStr);
            System.exit(1);
            return;
        }
        try {
            logger.info("Starting server at {}:{}", ip, port);
            JCFSocketServer server = new JCFSocketServer();
            server.start(port, ip);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Server shutting down.");
                server.stop();
            }));
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