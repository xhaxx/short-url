package com.shorturl.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 哈希工具
 * 用于长链接去重（取MD5前8位做 long_url_hash）
 */
public final class Md5Utils {

    private Md5Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 计算字符串的MD5，返回32位小写十六进制
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 取MD5前8位（用于 long_url_hash 去重）
     */
    public static String md5First8(String input) {
        return md5(input).substring(0, 8);
    }
}
