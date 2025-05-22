package com.appbuildersinc.attendance.source.Utilities;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class PasswordUtil {

    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    private static final int ITERATIONS = 1000; // High iteration count for security
    private static final int KEY_LENGTH = 256; // 256-bit key

    // Function to hash and store a password
    public static String hashPassword(String password) {
        try {
            byte[] salt = generateSalt(); // Generate a random salt
            byte[] hashedPassword = hashWithPBKDF2(password, salt);

            // Store salt and hash as a single string
            return Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hashedPassword);

        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // Function to verify a password against a stored hash
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            // Extract salt and hash from stored string
            String[] parts = storedHash.split("\\$");
            if (parts.length != 2) return false; // Invalid format

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHashedPassword = Base64.getDecoder().decode(parts[1]);

            // Hash the input password with the stored salt
            byte[] newHashedPassword = hashWithPBKDF2(password, salt);

            // Constant-time comparison to prevent timing attacks
            return constantTimeArrayCompare(storedHashedPassword, newHashedPassword);

        } catch (Exception e) {
            return false; // Verification failed
        }
    }

    // Generate a random salt
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // PBKDF2 hashing function
    private static byte[] hashWithPBKDF2(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    // Secure constant-time comparison to prevent timing attacks
    private static boolean constantTimeArrayCompare(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    // Test the functions
    public static void main(String[] args) {
        String password = "SecurePassword123";
        String hashedPassword = hashPassword(password);

        System.out.println("Stored Hash: " + hashedPassword);

        boolean isMatch = verifyPassword("SecurePassword123", hashedPassword);
        System.out.println("Password match: " + isMatch);

        boolean isWrongMatch = verifyPassword("WrongPassword", hashedPassword);
        System.out.println("Wrong password match: " + isWrongMatch);
    }
}

