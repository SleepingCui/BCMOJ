package org.bcmoj.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.utils.VersionUtil;
import org.apache.commons.cli.CommandLine;

/**
 * This class is the main entry point for the BCMOJ Judge Server.
 */
@Slf4j
public class Bootstrap {
    public static void run(String[] args) {
        printLogo();
        CommandLine cmd = CLIParser.parse(args);
        ServerInitializer.start(cmd);
    }
    public static void printLogo() {
        String version = VersionUtil.getVersion();
        String logo = String.format("""
          ____   ____ __  __  ___      _       _ ____\s
         | __ ) / ___|  \\/  |/ _ \\    | |     | / ___|  ___ _ ____   _____ _ __\s
         |  _ \\| |   | |\\/| | | | |_  | |  _  | \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|\s
         | |_) | |___| |  | | |_| | |_| | | |_| |___) |  __/ |   \\ V /  __/ |\s
         |____/ \\____|_|  |_|\\___/ \\___/   \\___/|____/ \\___|_|    \\_/ \\___|_|\s

        BCMOJ Judge Server v%s  Developed by SleepingCui & MxingFoew1034
      \s""", version);
        System.out.println(logo);
    }
}
