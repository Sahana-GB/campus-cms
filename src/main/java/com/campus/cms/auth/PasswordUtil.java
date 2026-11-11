
package com.campus.cms.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordUtil {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 10_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final HexFormat HEX = HexFormat.of();

    private PasswordUtil() {
        // Utility class: no objects needed.
    }

    // Generates a random salt and derives a password hash.
    public static String hashPassword(String password) {

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        byte[] hash = deriveHash(password, salt);

        return HEX.formatHex(salt) + ":" + HEX.formatHex(hash);
    }

    // Verifies a password against its stored salt:hash value.
    public static boolean verifyPassword(
            String password,
            String storedHash) {

        if (password == null || storedHash == null) {
            return false;
        }

        try {
            String[] parts = storedHash.split(":", -1);

            if (parts.length != 2) {
                return false;
            }

            byte[] salt = HEX.parseHex(parts[0]);
            byte[] expectedHash = HEX.parseHex(parts[1]);

            if (salt.length != SALT_LENGTH ||
                    expectedHash.length != HASH_LENGTH) {
                return false;
            }

            byte[] actualHash = deriveHash(password, salt);

            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] deriveHash(String password, byte[] salt) {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                HASH_LENGTH * 8
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Password hashing failed", e);

        } finally {
            spec.clearPassword();
        }
    }
}
