package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.netserver.SocketServer;
import org.bcmoj.utils.KeywordFileUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class ServerInitializer {

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static void start(Properties props, String configFilePath, boolean debug) {
        if (debug) {
            Configurator.setRootLevel(Level.DEBUG);
            log.debug("Debug enabled");
        } else {
            Configurator.setRootLevel(Level.INFO);
        }

        String host = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");

        boolean missingHost = isBlank(host);
        boolean missingPort = isBlank(portStr);
        boolean missingKwFile = isBlank(kwFile);

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

        if (isBlank(host) || isBlank(portStr) || isBlank(kwFile)) {
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
            SocketServer server = new SocketServer(host,port, kwFile);
            server.start();
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
