package com.github.caetanoog18.conectatea.support;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public final class TestCpf {
    private static final AtomicLong SEQUENCE = new AtomicLong(100_000_000L);

    private TestCpf() {
    }

    public static String next() {
        String base = String.format(Locale.ROOT, "%09d", SEQUENCE.getAndIncrement());
        String withFirstDigit = base + digit(base);

        return withFirstDigit + digit(withFirstDigit);
    }

    private static int digit(String value) {
        int total = 0;

        for (int i = 0; i < value.length(); i++) {
            total += (value.charAt(i) - '0') * (value.length() + 1 - i);
        }

        int result = (total * 10) % 11;

        return result == 10 ? 0 : result;
    }
}