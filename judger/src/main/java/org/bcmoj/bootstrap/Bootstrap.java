package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.VersionUtil;

import java.util.Properties;

@Slf4j
public class  Bootstrap {

    public static void run(String[] args) {
        printLogo();
        if (args.length == 0) {
            printHelp();
            return;
        }
        Properties cmdLineProps = CommandParser.parse(args);
        boolean debug = false;
        for (String arg : args) {
            if ("--debug".equalsIgnoreCase(arg)) {
                debug = true;
                break;
            }
        }
        String configFilePath = cmdLineProps.getProperty("config");
        Properties configFileProps = null;
        try {
            configFileProps = ConfigLoader.loadConfig(configFilePath);
        } catch (Exception e) {
            log.error("Error loading config file '{}': {}", configFilePath, e.getMessage());
            System.exit(1);
        }
        Properties Props = ConfigLoader.merge(configFileProps, cmdLineProps);
        ServerInitializer.start(Props, configFilePath, debug);
    }

    public static void printLogo() {
        String version = VersionUtil.getVersion();
        String logo = String.format("""
          ____   ____ __  __  ___      _       _ ____                     \s
         | __ ) / ___|  \\/  |/ _ \\    | |     | / ___|  ___ _ ____   _____ _ __
         |  _ \\| |   | |\\/| | | | |_  | |  _  | \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|
         | |_) | |___| |  | | |_| | |_| | | |_| |___) |  __/ |   \\ V /  __/ |\s
         |____/ \\____|_|  |_|\\___/ \\___/   \\___/|____/ \\___|_|    \\_/ \\___|_|\s

        BCMOJ Judge Server v%s  Developed by SleepingCui & MxingFoew1034
       \s""", version);
        System.out.println(logo);
    }

    public static void printHelp() {
        System.out.println("""
            BCMOJ Judge Server - Usage:

            Required (either via command-line or config file):
              --host=<IP>        Server IP address (e.g., 0.0.0.0)
              --port=<PORT>      Port number (e.g., 5000)
              --kwfile=<FILE>    Keyword file path

            Optional:
              --config=<FILE>    External config.properties path
              --debug            Enable debug mode

            Examples:
              java -jar code.jar --host=0.0.0.0 --port=5000 --kwfile=keywords.txt
              java -jar code.jar --config=config.properties
              java -jar code.jar --config=config.properties --host=192.168.1.1

            Note: Command line parameters have higher priority than config file values.
            """);
    }
}
