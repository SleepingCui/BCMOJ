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
        log.info("Starting BCMOJ Judge Server...");
        log.info("Version: {}", VersionUtil.getVersion());
        CommandLine cmd = CLIParser.parse(args);
        ServerInitialize.start(cmd);
    }

}
