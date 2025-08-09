package org.bcmoj.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for comparing program output against expected results.
 * <p>
 * Supports the following comparison modes:
 * <ul>
 *     <li><b>STRICT</b> – Exact match (character-by-character)</li>
 *     <li><b>IGNORE_SPACES</b> – Ignore extra spaces and line breaks</li>
 *     <li><b>CASE_INSENSITIVE</b> – Ignore letter case differences</li>
 *     <li><b>FLOAT_TOLERANT</b> – Compare floating-point numbers with a tolerance of {@code 1e-6}</li>
 * </ul>
 */
@Slf4j
public class OutputCompareUtil {

    public enum CompareMode {
        STRICT,
        IGNORE_SPACES,
        CASE_INSENSITIVE,
        FLOAT_TOLERANT
    }

    /**
     * Compares the actual output with the expected output using the specified comparison mode.
     *
     * @param actualOutput   the program's actual output
     * @param expectedOutput the expected output
     * @param mode           the comparison mode to use
     * @return {@code true} if the outputs match according to the mode, {@code false} otherwise
     */
    public static boolean compare(String actualOutput, String expectedOutput, CompareMode mode) {
        if (actualOutput == null || expectedOutput == null) {
            log.warn("Comparison failed: one of the outputs is null");
            return false;
        }
        log.debug("Comparison mode: {}, Expected output: {}, Actual output: {}", mode, expectedOutput, actualOutput);
        return switch (mode) {
            case STRICT -> expectedOutput.equals(actualOutput);
            case IGNORE_SPACES -> normalizeSpaces(expectedOutput).equals(normalizeSpaces(actualOutput));
            case CASE_INSENSITIVE -> expectedOutput.equalsIgnoreCase(actualOutput);
            case FLOAT_TOLERANT -> compareWithFloatTolerance(expectedOutput, actualOutput);
        };
    }

    /**
     * Compares the actual output with the expected output using {@link CompareMode#STRICT} mode.
     *
     * @param actualOutput   the program's actual output
     * @param expectedOutput the expected output
     * @return {@code true} if the outputs match exactly, {@code false} otherwise
     */
    public static boolean compare(String actualOutput, String expectedOutput) {
        return compare(actualOutput, expectedOutput, CompareMode.STRICT);
    }

    private static String normalizeSpaces(String str) {
        return str.trim().replaceAll("\\s+", " ");
    }

    private static boolean compareWithFloatTolerance(String expected, String actual) {
        String[] expectedTokens = expected.trim().split("\\s+");
        String[] actualTokens = actual.trim().split("\\s+");

        if (expectedTokens.length != actualTokens.length) {
            return false;
        }
        for (int i = 0; i < expectedTokens.length; i++) {
            String e = expectedTokens[i];
            String a = actualTokens[i];

            if (isNumeric(e) && isNumeric(a)) {
                double eVal = Double.parseDouble(e);
                double aVal = Double.parseDouble(a);
                double diff = Math.abs(eVal - aVal);
                log.debug("Comparing token {}: expected={} actual={} diff={}", i, eVal, aVal, diff);
                if (diff > 1.1E-6) {
                    return false;
                }
            } else if (!e.equals(a)) {
                return false;
            }
        }
        return true;
    }


    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
