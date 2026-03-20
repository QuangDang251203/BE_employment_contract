package com.be_employment_contract.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialUtilsTest {

    @Test
    void sanitizeUsernameSeedShouldKeepExpectedCharacters() {
        String value = CredentialUtils.sanitizeUsernameSeed("Test.User+1@example.com");
        assertEquals("test.user1", value);
    }

    @Test
    void generatePasswordShouldReturnConfiguredLength() {
        String password = CredentialUtils.generatePassword(10);
        assertNotNull(password);
        assertEquals(10, password.length());
        assertTrue(password.matches("[A-Za-z0-9@#]+"));
    }
}

