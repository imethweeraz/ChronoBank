package com.imeth.chronobank.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SecurityUtils class.
 */
public class SecurityUtilsTest {

    @Test
    public void testGenerateSalt() {
        String salt1 = SecurityUtils.generateSalt();
        String salt2 = SecurityUtils.generateSalt();
        
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2);
    }

    @Test
    public void testHashPassword() {
        String password = "securePassword123";
        String salt = SecurityUtils.generateSalt();
        
        String hash1 = SecurityUtils.hashPassword(password, salt);
        String hash2 = SecurityUtils.hashPassword(password, salt);
        
        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testVerifyPassword() {
        String password = "securePassword123";
        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword(password, salt);
        
        assertTrue(SecurityUtils.verifyPassword(password, hash, salt));
        assertFalse(SecurityUtils.verifyPassword("wrongPassword", hash, salt));
    }

    @Test
    public void testGenerateToken() {
        int length = 32;
        String token1 = SecurityUtils.generateToken(length);
        String token2 = SecurityUtils.generateToken(length);
        
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }
}