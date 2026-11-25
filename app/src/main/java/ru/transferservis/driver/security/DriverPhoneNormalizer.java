package ru.transferservis.driver.security;

public final class DriverPhoneNormalizer {

    private DriverPhoneNormalizer() {
    }

    public static String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String text =
                value.trim();

        if (text.isEmpty()) {
            return null;
        }

        StringBuilder digits =
                new StringBuilder();

        boolean leadingPlus =
                false;

        for (
                int index = 0;
                index < text.length();
                index++
        ) {

            char ch =
                    text.charAt(
                            index
                    );

            if (
                    ch == '+'
                            &&
                            index == 0
            ) {

                leadingPlus =
                        true;

                continue;
            }

            if (
                    ch >= '0'
                            &&
                            ch <= '9'
            ) {

                digits.append(
                        ch
                );

                continue;
            }

            if (
                    ch == ' '
                            ||
                            ch == '-'
                            ||
                            ch == '('
                            ||
                            ch == ')'
            ) {

                continue;
            }

            return null;
        }

        String raw =
                digits.toString();

        if (
                raw.length() == 10
                        &&
                        !leadingPlus
        ) {

            return "+7"
                    +
                    raw;
        }

        if (
                raw.length() == 11
                        &&
                        raw.charAt(0) == '8'
                        &&
                        !leadingPlus
        ) {

            return "+7"
                    +
                    raw.substring(
                            1
                    );
        }

        if (
                raw.length() == 11
                        &&
                        raw.charAt(0) == '7'
        ) {

            return "+"
                    +
                    raw;
        }

        if (
                leadingPlus
                        &&
                        raw.length() >= 8
                        &&
                        raw.length() <= 15
                        &&
                        raw.charAt(0) != '0'
        ) {

            return "+"
                    +
                    raw;
        }

        return null;
    }
}
