package com.be_employment_contract.constant;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String REDIS_OTP_PREFIX = "otp";
    public static final int OTP_LENGTH = 6;
    public static final long OTP_TTL_MINUTES = 5L;
    public static final int INITIAL_PASSWORD_LENGTH = 10;

    public static final String EMAIL_SUBJECT_CREDENTIAL = "Employment Contract - Account Information";
    public static final String EMAIL_SUBJECT_OTP = "Employment Contract - OTP Verification";
}

