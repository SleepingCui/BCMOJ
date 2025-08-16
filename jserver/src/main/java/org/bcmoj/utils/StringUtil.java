package org.bcmoj.utils;

import java.util.Map;

/**
 * Utility class for string manipulation functions.
 * <p>
 * Provides methods for processing and transforming strings, such as unescaping escape sequences.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * String escaped = "Hello\\nWorld";
 * String unescaped = StringUtil.unescapeString(escaped);
 * // unescaped == "Hello
 * // World"
 * </pre>
 * </p>
 *
 * @author SleepingCui
 * @version ${project.version}
 */
public class StringUtil {

    private static final Map<Character, Character> ESCAPE_MAP = Map.of(
            'n', '\n', 't', '\t', 'r', '\r',
            '\\', '\\', '\"', '\"', '\'', '\''
    );

    /**
     * Converts escape sequences in the input string into their actual characters.
     * <p>
     * Supported escape sequences: \n, \t, \r, \\, \", \'
     * </p>
     *
     * @param str the input string containing escape sequences, may be null
     * @return the unescaped string, or null if the input was null
     */
    public static String unescapeString(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length() && ESCAPE_MAP.containsKey(str.charAt(i + 1))) {
                sb.append(ESCAPE_MAP.get(str.charAt(++i)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
