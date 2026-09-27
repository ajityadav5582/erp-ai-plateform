package com.erp.platform.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class UserRoleTest {

    private static final Instant ASSIGNED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-12-31T23:59:59Z");

    @Test
    public void usesLongIdentityAndPreservesAssignmentMetadata() {
        UserRole assignment = UserRole.assign(
                10L,
                20L,
                30L,
                "admin",
                ASSIGNED_AT,
                EXPIRES_AT,
                true);
        assignment.setId(40L);

        assertEquals(40L, assignment.getId());
        assertEquals(10L, assignment.getUserId());
        assertEquals(20L, assignment.getRoleId());
        assertEquals(30L, assignment.getTenantId());
        assertEquals("admin", assignment.getAssignedBy());
        assertEquals(ASSIGNED_AT, assignment.getAssignedAt());
        assertEquals(EXPIRES_AT, assignment.getExpiresAt());
        assertTrue(assignment.isPrimaryRole());
    }

    @Test
    public void supportsMultipleRolesPerUser() {
        UserRole firstRole = UserRole.assign(
                10L,
                20L,
                30L,
                "admin",
                ASSIGNED_AT,
                null,
                false);
        UserRole secondRole = UserRole.assign(
                10L,
                21L,
                30L,
                "admin",
                ASSIGNED_AT,
                null,
                false);

        assertNotEquals(firstRole.getRoleId(), secondRole.getRoleId());
        assertTrue(firstRole.isActive());
        assertTrue(secondRole.isActive());
    }

    @Test
    public void preservesExpirationAndRevocationBehavior() {
        UserRole assignment = UserRole.assign(
                10L,
                20L,
                30L,
                "admin",
                ASSIGNED_AT,
                EXPIRES_AT,
                false);

        assertTrue(assignment.isActive());
        assertFalse(assignment.isRevoked());
        assertFalse(assignment.isExpired());

        assignment.extendExpiration(EXPIRES_AT.plusSeconds(3600));
        assertEquals(EXPIRES_AT.plusSeconds(3600), assignment.getExpiresAt());
        assignment.removeExpiration();
        assertNull(assignment.getExpiresAt());
        assertTrue(assignment.isPermanent());

        assignment.revoke(ASSIGNED_AT.plusSeconds(1), "admin", "No longer required");
        assertFalse(assignment.isActive());
        assertTrue(assignment.isRevoked());
        assertEquals("No longer required", assignment.getRevokeReason());
    }

    @Test
    public void preventsExpirationChangesAfterRevocation() {
        UserRole assignment = UserRole.assign(
                10L,
                20L,
                30L,
                "admin",
                ASSIGNED_AT,
                EXPIRES_AT,
                false);
        assignment.revoke(ASSIGNED_AT.plusSeconds(1), "admin", "No longer required");

        assertThrows(IllegalStateException.class,
                () -> assignment.extendExpiration(EXPIRES_AT.plusSeconds(3600)));
        assertThrows(IllegalStateException.class, assignment::removeExpiration);
    }

    @Test
    public void rejectsMissingAssignmentReferences() {
        assertThrows(IllegalArgumentException.class,
                () -> UserRole.assign(null, 20L, 30L, "admin", ASSIGNED_AT, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> UserRole.assign(10L, null, 30L, "admin", ASSIGNED_AT, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> UserRole.assign(10L, 20L, null, "admin", ASSIGNED_AT, null, false));
    }
}
