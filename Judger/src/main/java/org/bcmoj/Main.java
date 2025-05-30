package org.bcmoj;

import org.bcmoj.config.ConfigProcess;
import org.bcmoj.netserver.JCFSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void on_start(){
        String port = ConfigProcess.GetConfig("ServerPort");
        try {
            logger.info("Starting server on port {}...", port);
            JCFSocketServer server = new JCFSocketServer();
            server.start(Integer.parseInt(port));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down server...");
                server.stop();
            }));
        } catch (Exception e) {
            logger.error("Server fatal error: ", e);
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
    public static void main(String[] args) {
        logo();
        on_start();
    }
}