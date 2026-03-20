package com.be_employment_contract.utils;

public final class RedisKeyUtils {

    private RedisKeyUtils() {
    }

    public static String otpKey(String prefix, String username, String contractCode) {
        return prefix + ":" + username + ":" + contractCode;
    }

    public static String otpKey(String prefix, String username, Long contractId) {
        return prefix + ":" + username + ":" + contractId;
    }
}

