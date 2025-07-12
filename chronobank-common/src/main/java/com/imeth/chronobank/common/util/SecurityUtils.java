package com.imeth.chronobank.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for security-related operations.
 */
public final class SecurityUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 16;

    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates a secure random salt.
     *
     * @return Base64 encoded salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password with a salt using SHA-256.
     *
     * @param password the password to hash
     * @param salt     the salt to use
     * @return the hashed password
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verifies a password against a stored hash and salt.
     *
     * @param password     the password to verify
     * @param storedHash   the stored hash
     * @param salt         the salt used for hashing
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash, String salt) {
        String hashedPassword = hashPassword(password, salt);
        return storedHash.equals(hashedPassword);
    }

    /**
     * Generates a secure random token.
     *
     * @param length the length of the token
     * @return the generated token
     */
    public static String generateToken(int length) {
        byte[] token = new byte[length];
        SECURE_RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}