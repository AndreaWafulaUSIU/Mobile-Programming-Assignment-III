package com.example.atm;

import java.util.Locale;

/** Small helpers shared by all screens. */
public class Util {

    /** Extra key used to pass the logged-in account number between activities. */
    public static final String EXTRA_ACC_NO = "ACC_NO";

    /** Maximum amount that can be withdrawn in one transaction (KES). */
    public static final long WITHDRAW_LIMIT = 40000;

    /** Formats an amount as, for example, "KES 12,500". */
    public static String money(long amount) {
        return String.format(Locale.US, "KES %,d", amount);
    }

    /** Parses a whole number. Returns -1 if the text is empty or not a valid number. */
    public static long parseNumber(String text) {
        if (text == null) return -1;
        text = text.trim();
        if (text.isEmpty()) return -1;
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** A PIN is exactly four digits. */
    public static boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{4}");
    }
}
