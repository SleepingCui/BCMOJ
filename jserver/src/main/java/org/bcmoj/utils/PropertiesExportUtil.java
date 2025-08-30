package org.bcmoj.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class PropertiesExportUtil {

    /**
     * Exports the command-line parsed Properties to a .properties file.
     * <p>
     * This method does not read any configuration file; it only uses the
     * Properties obtained from command-line arguments.
     * </p>
     *
     * @param cmdLineProps Properties parsed from the command-line arguments.
     *                     If null, an empty Properties object will be used.
     */
    public static void export(Properties cmdLineProps) {
        if (cmdLineProps == null) cmdLineProps = new Properties();
        String host = cmdLineProps.getProperty("host", "");
        String port = cmdLineProps.getProperty("port", "");
        String kwFile = cmdLineProps.getProperty("kwfile", "");
        String compilerPath = cmdLineProps.getProperty("comp", cmdLineProps.getProperty("CompilerPath", "g++"));
        String cppStandard = cmdLineProps.getProperty("std", cmdLineProps.getProperty("CppStandard", "c++11"));

        Properties propsToExport = new Properties();
        propsToExport.setProperty("ServerIP", host);
        propsToExport.setProperty("ServerPort", port);
        propsToExport.setProperty("KeywordsFile", kwFile);
        propsToExport.setProperty("CompilerPath", compilerPath);
        propsToExport.setProperty("CppStandard", cppStandard);

        try (FileOutputStream fos = new FileOutputStream("exported_config.properties")) {
            propsToExport.store(fos, "Exported BCMOJ Server Properties (from command line only)");
            log.info("Properties exported successfully to exported_config.properties");
        } catch (IOException e) {
            log.error("Failed to export properties: {}", e.getMessage());
        }
    }
}