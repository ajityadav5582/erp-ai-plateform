package com.erp.platform.common.utils;

import java.util.UUID;

/**
 * Utility class for UUID operations.
 *
 * @since 1.0.0
 */
public final class UuidUtils {

    private UuidUtils() {
        // Utility class
    }

    /**
     * Generates a new random UUID.
     *
     * @return a new UUID
     */
    public static UUID generate() {
        return UUID.randomUUID();
    }

    /**
     * Generates a new UUID string.
     *
     * @return a new UUID as string
     */
    public static String generateString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Checks if a string is a valid UUID.
     *
     * @param value the string to check
     * @return true if valid UUID, false otherwise
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses a string to UUID.
     *
     * @param value the string to parse
     * @return the parsed UUID
     * @throws IllegalArgumentException if the string is not a valid UUID
     */
    public static UUID parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UUID string cannot be null or blank");
        }
        return UUID.fromString(value);
    }

    /**
     * Converts a UUID to a byte array.
     *
     * @param uuid the UUID to convert
     * @return the byte array representation
     */
    public static byte[] toBytes(UUID uuid) {
        if (uuid == null) {
            return new byte[0];
        }
        byte[] bytes = new byte[16];
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (most >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (least >>> (8 * (7 - i)));
        }
        return bytes;
    }

    /**
     * Converts a byte array to UUID.
     *
     * @param bytes the byte array to convert
     * @return the UUID
     * @throws IllegalArgumentException if the byte array is not 16 bytes
     */
    public static UUID fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("Byte array must be 16 bytes");
        }
        long most = 0;
        long least = 0;
        for (int i = 0; i < 8; i++) {
            most = (most << 8) | (bytes[i] & 0xff);
            least = (least << 8) | (bytes[8 + i] & 0xff);
        }
        return new UUID(most, least);
    }

    /**
     * Creates a UUID from a string, returning null if invalid.
     *
     * @param value the string to parse
     * @return the UUID or null if invalid
     */
    public static UUID safeParse(String value) {
        if (!isValid(value)) {
            return null;
        }
        return parse(value);
    }
}
