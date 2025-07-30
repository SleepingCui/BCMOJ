package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.netserver.SocketServer;
import org.bcmoj.utils.KeywordFileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class ServerInitializer {

    public static void start(Properties props, String configFilePath) {

        log.info("Operating System: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Working Directory: {}", System.getProperty("user.dir"));

        String host = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");
        boolean missingHost = host.isBlank();
        boolean missingPort = portStr.isBlank();
        boolean missingKwFile = kwFile.isBlank();

        if (configFilePath != null && (missingHost || missingPort || missingKwFile)) {
            Properties fileProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFilePath)) {
                fileProps.load(fis);
                if (missingHost) host = fileProps.getProperty("ServerIP");
                if (missingPort) portStr = fileProps.getProperty("ServerPort");
                if (missingKwFile) kwFile = fileProps.getProperty("KeywordsFile");
                log.info("Loaded missing parameters from config file: {}", configFilePath);
            } catch (IOException e) {
                log.error("Failed to load config file '{}': {}", configFilePath, e.getMessage());
                System.exit(1);
            }
        }

        if (host.isBlank() || portStr.isBlank() || kwFile.isBlank()) {
            log.error("Missing required parameters: host={}, port={}, kwfile={}", host, portStr, kwFile);
            Bootstrap.printHelp();
            System.exit(1);
        }

        try {
            if (KeywordFileUtil.createDefaultIfNotExists(kwFile)) {
                log.info("Keyword file not found, created default keyword file at: {}", kwFile);
            }
        } catch (IOException e) {
            log.warn("Could not create keyword file '{}': {}", kwFile, e.getMessage());
        }

        try {
            int port = Integer.parseInt(portStr);
            SocketServer server = new SocketServer();
            server.start(port, host, kwFile);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (NumberFormatException e) {
            log.error("Invalid port number: {}", portStr);
            System.exit(1);
        } catch (Exception e) {
            log.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }
}
