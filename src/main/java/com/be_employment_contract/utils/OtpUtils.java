package com.be_employment_contract.utils;

import java.security.SecureRandom;

public final class OtpUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpUtils() {
    }

    public static String generateNumericOtp(int length) {
        int bound = (int) Math.pow(10, length);
        int value = RANDOM.nextInt(bound);
        return String.format("%0" + length + "d", value);
    }
}

