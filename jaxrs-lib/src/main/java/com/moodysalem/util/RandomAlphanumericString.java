package com.moodysalem.util;

import java.security.SecureRandom;

/**
 * Provides helper methods for generating a random string using SecureRandom
 */
public class RandomAlphanumericString {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final char[] SYMBOLS;
    private static final int NUM_SYMBOLS;

    static {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch) {
            tmp.append(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ++ch) {
            tmp.append(ch);
        }
        for (char ch = 'A'; ch <= 'Z'; ++ch) {
            tmp.append(ch);
        }
        SYMBOLS = tmp.toString().toCharArray();
        NUM_SYMBOLS = SYMBOLS.length;
    }

    public static String get(int length) {
        StringBuilder sb = new StringBuilder();
        if (length < 0) {
            throw new IllegalArgumentException();
        }
        while (length > 0) {
            length--;
            sb.append(SYMBOLS[SECURE_RANDOM.nextInt(NUM_SYMBOLS)]);
        }
        return sb.toString();
    }
}
