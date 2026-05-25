package com.sealhackathon.api.auth.util;

import java.security.SecureRandom;

/**
 * Sinh mật khẩu tạm readable cho judge khách (gửi một lần qua email).
 */
public final class TempPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int DEFAULT_LENGTH = 12;
    private static final SecureRandom RNG = new SecureRandom();

    private TempPasswordGenerator() {
    }

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
