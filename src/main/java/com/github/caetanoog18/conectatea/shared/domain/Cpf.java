package com.github.caetanoog18.conectatea.shared.domain;

public final class Cpf {
    private Cpf() {
    }

    public static String normalize(String value) {
        if (value == null) {
            throw new InvalidCpfException();
        }

        String trimmed = value.trim();

        if (!trimmed.matches(
                "(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})"
        )) {
            throw new InvalidCpfException();
        }

        String digits = trimmed.replace(".", "").replace("-", "");

        if (digits.chars().distinct().count() == 1) {
            throw new InvalidCpfException();
        }

        if (checkDigit(digits, 9) != digits.charAt(9) - '0' || checkDigit(digits, 10) != digits.charAt(10) - '0') {
            throw new InvalidCpfException();
        }

        return digits;
    }

    private static int checkDigit(String digits, int length) {
        int total = 0;

        for (int i = 0; i < length; i++) {
            total += (digits.charAt(i) - '0') * (length + 1 - i);
        }

        int digit = (total * 10) % 11;

        return digit == 10 ? 0 : digit;
    }
}