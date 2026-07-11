package com.erp.platform.identity.application.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic helper for token storage.
 *
 * <p>Refresh and password-reset tokens are opaque, high-entropy strings. Only their
 * SHA-256 hash is persisted, so a database compromise does not expose usable tokens.
 *
 * @since 1.0.0
 */
public final class TokenHasher {

    private TokenHasher() {
        // utility class
    }

    /**
     * Computes the hex-encoded SHA-256 hash of the given raw token.
     *
     * @param rawToken the raw token value (never stored)
     * @return lower-case hex SHA-256 digest (64 characters)
     */
    public static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS and always available.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
