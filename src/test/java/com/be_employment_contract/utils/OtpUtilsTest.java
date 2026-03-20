package com.be_employment_contract.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpUtilsTest {

    @Test
    void generateNumericOtpShouldMatchExpectedLengthAndDigits() {
        String otp = OtpUtils.generateNumericOtp(6);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }
}

