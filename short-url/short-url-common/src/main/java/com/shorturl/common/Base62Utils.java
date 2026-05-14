package com.shorturl.common;

/**
 * Base62 编解码工具
 * 字符集: 0-9 A-Z a-z (62个字符，URL安全)
 */
public final class Base62Utils {

    private static final char[] CHARS = new char[62];
    private static final int[] INDEX = new int[128];

    static {
        // 初始化字符集: 0-9(0..9), A-Z(10..35), a-z(36..61)
        int pos = 0;
        for (char c = '0'; c <= '9'; c++) {
            CHARS[pos] = c;
            INDEX[c] = pos++;
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            CHARS[pos] = c;
            INDEX[c] = pos++;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            CHARS[pos] = c;
            INDEX[c] = pos++;
        }
    }

    private Base62Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 数字ID → Base62短码
     * 例: 1→"1", 62→"10", 100→"1C"
     */
    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (id == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder(16);
        long x = id;
        while (x > 0) {
            sb.append(CHARS[(int) (x % 62)]);
            x /= 62;
        }
        return sb.reverse().toString();
    }

    /**
     * Base62短码 → 数字ID
     */
    public static long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code must not be empty");
        }
        long result = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c >= INDEX.length || INDEX[c] < 0) {
                throw new IllegalArgumentException("Invalid Base62 char: " + c);
            }
            result = result * 62 + INDEX[c];
        }
        return result;
    }
}
