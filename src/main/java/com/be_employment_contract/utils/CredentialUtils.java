package com.be_employment_contract.utils;

import java.security.SecureRandom;

public final class CredentialUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";

    private CredentialUtils() {
    }

    public static String sanitizeUsernameSeed(String email) {
        String localPart = email.split("@")[0].toLowerCase();
        return localPart.replaceAll("[^a-z0-9._]", "");
    }

    public static String generatePassword(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(PASSWORD_CHARS.length());
            builder.append(PASSWORD_CHARS.charAt(index));
        }
        return builder.toString();
    }
}

