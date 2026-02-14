package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.config.ServerConfig;
import org.bcmoj.netserver.SocketServer;

/**
 * Launches the BCMOJ Judge Server based on a provided ServerConfig.
 * Handles final configuration adjustments (like security checks) and server lifecycle management.
 */
@Slf4j
public class ServerLauncher {

    /**
     * Launches the server using the provided configuration.
     * Applies any final logic to the config and starts the SocketServer.
     * Sets up a shutdown hook.
     *
     * @param config The server configuration object.
     */
    public static void launch(ServerConfig config) {
        try {
            ServerConfig finalConfig = Initialize.processConfig(config);

            log.info("Initializing server...");
            SocketServer server = new SocketServer(finalConfig);
            server.start(finalConfig.getNettyThreads());

            Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "Server-Shutdown-Hook"));

            log.info("Server launched and running with config: {}", finalConfig);

        } catch (Exception e) {
            log.error("Failed to initialize or launch server with config: {} - {}", config, e.getMessage());
            System.exit(1);
        }
    }
}