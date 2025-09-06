package org.bcmoj.utils;

import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtil {
    /**
     * Recursively deletes the specified file or directory.
     * If the file is a directory, all its contents will be deleted as well.
     * Logs a warning if a file or directory cannot be deleted.
     *
     * @param file the file or directory to delete
     */
    public static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            log.warn("Failed to delete: {}", file.getAbsolutePath());
        } else {
            log.debug("Deleted: {}", file.getAbsolutePath());
        }
    }
}