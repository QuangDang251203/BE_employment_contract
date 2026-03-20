package com.be_employment_contract.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    @Test
    void hashAndMatchesShouldWorkForValidPassword() {
        String raw = "P@ssw0rd1";
        String hashed = PasswordUtils.hash(raw);

        assertNotNull(hashed);
        assertTrue(PasswordUtils.matches(raw, hashed));
        assertFalse(PasswordUtils.matches("wrong", hashed));
    }
}

