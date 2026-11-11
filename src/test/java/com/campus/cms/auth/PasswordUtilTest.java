
package com.campus.cms.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void generatedHashHasCorrectFormat() {
        String hash = PasswordUtil.hashPassword("hello123");

        String[] parts = hash.split(":");

        assertEquals(2, parts.length);
        assertEquals(32, parts[0].length());
        assertEquals(64, parts[1].length());
    }

    @Test
    void correctPasswordShouldVerify() {
        String hash = PasswordUtil.hashPassword("hello123");

        assertTrue(PasswordUtil.verifyPassword("hello123", hash));
    }

    @Test
    void incorrectPasswordShouldFail() {
        String hash = PasswordUtil.hashPassword("hello123");

        assertFalse(PasswordUtil.verifyPassword("wrong123", hash));
    }

    @Test
    void samePasswordShouldGenerateDifferentHashes() {
        String hash1 = PasswordUtil.hashPassword("hello123");
        String hash2 = PasswordUtil.hashPassword("hello123");

        assertNotEquals(hash1, hash2);
    }
}
