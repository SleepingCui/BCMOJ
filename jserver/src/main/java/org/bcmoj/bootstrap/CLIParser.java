package org.bcmoj.bootstrap;

import org.apache.commons.cli.*;
import java.util.Properties;

public class CLIParser {
    private static final Options options;

    static {
        options = new Options();
        options.addOption(Option.builder().longOpt("help").desc("Print this help message showing all available options and usage instructions").build());
        options.addOption(Option.builder().longOpt("debug").desc("Enable debug mode to show detailed logging output for troubleshooting purposes").build());
        options.addOption(Option.builder().longOpt("extract").desc("Export the current configuration properties to a file instead of starting the server").build());
        options.addOption(Option.builder().longOpt("host").hasArg().argName("IP").desc("Server IP address to bind to (e.g., 0.0.0.0 for all interfaces)").build());
        options.addOption(Option.builder().longOpt("port").hasArg().argName("PORT").desc("Port number for the server to listen on (e.g., 12345)").build());
        options.addOption(Option.builder().longOpt("kwfile").hasArg().argName("FILE").desc("Path to the keyword file containing reserved words for the judge system").build());
        options.addOption(Option.builder().longOpt("config").hasArg().argName("FILE").desc("Optional path to a configuration file to load additional server properties").build());
        options.addOption(Option.builder().longOpt("comp").hasArg().argName("FILE").desc("Path to the C++ compiler executable to use for code compilation (default: g++)").build());
        options.addOption(Option.builder().longOpt("std").hasArg().argName("STD").desc("C++ standard to use for compilation (e.g., c++11, c++17; default: c++11)").build());
    }
    public static CommandLine parse(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            printHelp();
            System.err.println("Error parsing command line: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }
    public static Properties toProperties(CommandLine cmd) {
        Properties props = new Properties();
        if (cmd.hasOption("host")) props.setProperty("host", cmd.getOptionValue("host"));
        if (cmd.hasOption("port")) props.setProperty("port", cmd.getOptionValue("port"));
        if (cmd.hasOption("kwfile")) props.setProperty("kwfile", cmd.getOptionValue("kwfile"));
        if (cmd.hasOption("config")) props.setProperty("config", cmd.getOptionValue("config"));
        if (cmd.hasOption("comp")) props.setProperty("CompilerPath", cmd.getOptionValue("comp"));
        if (cmd.hasOption("std")) props.setProperty("CppStandard", cmd.getOptionValue("std"));
        if (cmd.hasOption("debug")) props.setProperty("debug", "true");
        if (cmd.hasOption("extract")) props.setProperty("extract", "true");
        return props;
    }
    public static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar BCMOJ.jar", options, true);
    }
}
