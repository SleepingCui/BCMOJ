package org.bcmoj.netserver;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.ConfigProcess;
import org.bcmoj.utils.KWFileWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class Bootstrap {

    public static void run(String[] args) {
        printLogo();
        if (args.length == 0) {
            printHelp();
            return;
        }
        Properties props = parseArguments(args);
        startServer(props);
    }

    private static void printHelp() {
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

    private static void printLogo() {
        String logo = """
                  ____   ____ __  __  ___      _       _ ____                       \s
                 | __ ) / ___|  \\/  |/ _ \\    | |     | / ___|  ___ _ ____   _____ _ __
                 |  _ \\| |   | |\\/| | | | |_  | |  _  | \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|
                 | |_) | |___| |  | | |_| | |_| | | |_| |___) |  __/ |   \\ V /  __/ |  \s
                 |____/ \\____|_|  |_|\\___/ \\___/   \\___/|____/ \\___|_|    \\_/ \\___|_|  \s

                BCMOJ Judge Server v1.0-SNAPSHOT  Developed by SleepingCui & MxingFoew1034
               \s""";
        System.out.println(logo);
    }

    private static Properties parseArguments(String[] args) {
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
                log.info("Loaded config file: {}", configFilePath);
            } catch (IOException e) {
                log.error("Error loading config file '{}': {}", configFilePath, e.getMessage());
                System.exit(1);
            }
        }

        cmdLineProps.forEach((k, v) -> finalProps.setProperty(k.toString(), v.toString()));
        return finalProps;
    }

    private static void startServer(Properties props) {
        String ip = props.getProperty("host");
        String portStr = props.getProperty("port");
        String kwFile = props.getProperty("kwfile");

        boolean missing = isBlank(ip) || isBlank(portStr) || isBlank(kwFile);

        if (missing) {
            log.info("Loading missing parameters from default config...");
            if (isBlank(ip)) ip = ConfigProcess.GetConfig("ServerIP");
            if (isBlank(portStr)) portStr = ConfigProcess.GetConfig("ServerPort");
            if (isBlank(kwFile)) kwFile = ConfigProcess.GetConfig("KeywordsFile");
        }

        if (isBlank(ip) || isBlank(portStr)) {
            log.error("Missing required parameters: host={}, port={}", ip, portStr);
            printHelp();
            System.exit(1);
        }

        if (isBlank(kwFile)) {
            kwFile = "keywords.txt";
            log.info("No keyword file specified, using default: {}", kwFile);
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
