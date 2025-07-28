package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.netserver.SocketServer;
import org.bcmoj.utils.KWFileWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class ServerInitializer {

    public static void start(Properties props, String configFilePath) {
        String ip = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");

        boolean missingHost = isBlank(ip);
        boolean missingPort = isBlank(portStr);
        boolean missingKwFile = isBlank(kwFile);

        if (configFilePath != null && (missingHost || missingPort || missingKwFile)) {
            Properties fileProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                fileProps.load(fis);
                if (missingHost) ip = fileProps.getProperty("ServerIP");
                if (missingPort) portStr = fileProps.getProperty("ServerPort");
                if (missingKwFile) kwFile = fileProps.getProperty("KeywordsFile");
                log.info("Loaded missing params from config file: {}", configFilePath);
            } catch (IOException e) {
                log.error("Failed to load config file '{}': {}", configFilePath, e.getMessage());
                System.exit(1);
            }
        }

        if (isBlank(ip) || isBlank(portStr) || isBlank(kwFile)) {
            log.error("Missing required parameters: host={}, port={}, kwfile={}", ip, portStr, kwFile);
            Bootstrap.printHelp();
            System.exit(1);
        }

        try {
            if (KWFileWriter.createDefaultIfNotExists(kwFile)) {
                log.info("Keyword file not found, created default keyword file at: {}", kwFile);
            }
        } catch (IOException e) {
            log.warn("Could not create keyword file '{}': {}", kwFile, e.getMessage());
        }

        try {
            int port = Integer.parseInt(portStr);
            SocketServer server = new SocketServer();
            log.info("Starting server on {}:{} ... ",ip, port);
            server.start(port, ip, kwFile);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (NumberFormatException e) {
            log.error("Invalid port number: {}", portStr);
            System.exit(1);
        } catch (Exception e) {
            log.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
