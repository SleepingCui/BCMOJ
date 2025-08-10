package org.bcmoj.utils;

import java.util.Map;

public class StringUtil {
    private static final Map<Character, Character> ESCAPE_MAP = Map.of(
            'n', '\n', 't', '\t', 'r', '\r',
            '\\', '\\', '\"', '\"', '\'', '\''
    );

    public static String unescapeString(String str) {
        if (str == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length() && ESCAPE_MAP.containsKey(str.charAt(i + 1))) {
                sb.append(ESCAPE_MAP.get(str.charAt(++i)));
            } else sb.append(c);
        }
        return sb.toString();
    }
}