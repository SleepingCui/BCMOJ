package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.VersionUtil;
import org.bcmoj.utils.PropertiesExportUtil;

import java.util.Properties;

/**
 * Entry point and main bootstrap class for the BCMOJ Judge Server.
 * <p>
 * This class handles command-line argument parsing, configuration loading,
 * debug mode, optional compiler path, and server initialization. It also
 * supports exporting command-line properties to a .properties file via
 * the {@code --extract} option.
 * </p>
 */
@Slf4j
public class Bootstrap {

    /**
     * Main runner method for the server.
     * <p>
     * Parses command-line arguments, loads configuration from file if provided,
     * merges properties, determines debug mode and optional compiler path,
     * and starts the server or exports the properties if {@code --extract} is used.
     *
     * @param args Command-line arguments passed to the server
     */
    public static void run(String[] args) {
        printLogo();

        if (args.length == 0) {
            printHelp();
            return;
        }
        Properties cmdLineProps = CommandParser.parse(args);
        boolean debug = false;
        boolean extract = false;

        for (String arg : args) {
            if ("--debug".equalsIgnoreCase(arg)) {
                debug = true;
            }
            if ("--extract".equalsIgnoreCase(arg)) {
                extract = true;
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
        Properties props = ConfigLoader.merge(configFileProps, cmdLineProps);

        // Determine compiler path: command-line > properties > default g++
        String compilerPath = cmdLineProps.getProperty("comp");
        if (compilerPath == null || compilerPath.isBlank()) {
            compilerPath = props.getProperty("CompilerPath");
        }
        if (compilerPath == null || compilerPath.isBlank()) {
            compilerPath = "g++";
        }
        props.setProperty("CompilerPath", compilerPath);
        if (extract) {
            PropertiesExportUtil.export(cmdLineProps);
            return;
        }
        ServerInitializer.start(props, configFilePath, debug);
    }

    public static void printLogo() {
        String version = VersionUtil.getVersion();
        String logo = String.format("""
          ____   ____ __  __  ___      _       _ ____                   \s
         | __ ) / ___|  \\/  |/ _ \\    | |     | / ___|  ___ _ ____   _____ _ __
         |  _ \\| |   | |\\/| | | | |_  | |  _  | \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|
         | |_) | |___| |  | | |_| | |_| | | |_| |___) |  __/ |   \\ V /  __/ |
         |____/ \\____|_|  |_|\\___/ \\___/   \\___/|____/ \\___|_|    \\_/ \\___|_|

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
              --comp=<FILE>      Compiler path (overrides CompilerPath in config)
              --debug            Enable debug mode
              --extract          Export merged properties to a .properties file

            Examples:
              java -jar code.jar --host=0.0.0.0 --port=5000 --kwfile=keywords.txt
              java -jar code.jar --config=config.properties
              java -jar code.jar --config=config.properties --host=192.168.1.1 --comp=/usr/bin/g++
              java -jar code.jar --host=0.0.0.0 --port=12345 --kwfile=keywords.txt --debug --comp=gcc --extract

            Note: Command line parameters have higher priority than config file values.
            """);
    }
}
