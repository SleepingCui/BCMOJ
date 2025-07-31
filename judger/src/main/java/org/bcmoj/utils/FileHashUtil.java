package org.bcmoj.utils;

import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHashUtil {
    /**
     * Calculates the SHA-256 hash of the specified file.
     *
     * @param file the file to calculate the hash for
     * @return the SHA-256 hash as a lowercase hexadecimal string
     * @throws IOException if an I/O error occurs while reading the file
     * @throws NoSuchAlgorithmException if SHA-256 algorithm is not available
     */
    public static String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return Hex.encodeHexString(digest.digest());
    }
}
