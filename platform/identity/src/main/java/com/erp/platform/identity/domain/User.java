package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * User aggregate root for identity and access management.
 *
 * <p>Represents a user in the multi-tenant ERP platform.
 * This is the central entity that owns user identity, credentials, and lifecycle.
 *
 * <p>The aggregate follows Clean Architecture and DDD principles:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Encapsulates all user business logic and invariants</li>
 *   <li>Does not expose mutable state directly (no setters for business fields)</li>
 *   <li>Provides domain behavior methods for state transitions</li>
 * </ul>
 *
 * <p><strong>Lifecycle Rules:</strong>
 * <ul>
 *   <li>PENDING_ACTIVATION → ACTIVE (via activate)</li>
 *   <li>ACTIVE → LOCKED (via lock)</li>
 *   <li>LOCKED → ACTIVE (via unlock)</li>
 *   <li>Any state → DEACTIVATED (via deactivate)</li>
 *   <li>Any state → ARCHIVED (terminal state, via archive)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class User extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * The unique business identifier for the user.
     * Used for API access, external references, and audit trails.
     * This is a UUID (not String) as per requirements.
     */
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    /**
     * Reference to the tenant this user belongs to.
     * Ensures multi-tenant data isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /**
     * Unique username for authentication within the tenant.
     * Must be unique per tenant.
     */
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /**
     * Email address of the user.
     * Used for notifications, password reset, and communication.
     * Must be unique per tenant.
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Hashed password of the user.
     * Never store plain text passwords. Use BCrypt or similar.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * First name of the user.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Last name of the user.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Phone number of the user.
     * Used for notifications and contact purposes.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Job title or position of the user.
     * Used for organizational context and reporting.
     */
    @Column(name = "job_title", length = 100)
    private String jobTitle;

    /**
     * URL to the user's profile image.
     * Used for displaying user avatars in the UI.
     */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * Current status of the user.
     * Controls access to the platform and available features.
     */
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    /**
     * Timestamp when the user was last logged in.
     * Updated on each successful authentication.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Timestamp when the user account was locked.
     * Set when transitioning to LOCKED status.
     */
    @Column(name = "locked_at")
    private Instant lockedAt;

    /**
     * Reason for locking the user account.
     * Used for audit and compliance purposes.
     */
    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    /**
     * Timestamp when the user was activated.
     * Set when transitioning to ACTIVE status.
     */
    @Column(name = "activated_at")
    private Instant activatedAt;

    /**
     * Timestamp when the user was deactivated.
     * Set when transitioning to DEACTIVATED status.
     */
    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    /**
     * Number of consecutive failed login attempts since the last successful login.
     * Used to enforce temporary account lockout after repeated failures.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts;

    /**
     * Instant until which the account is temporarily locked due to failed login
     * attempts. Null when the account is not temporarily locked.
     * Distinct from the {@link UserStatus#LOCKED} lifecycle state, which is an
     * administrative/manual lock.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * Reference to the branch this user belongs to.
     * Nullable for users not assigned to a specific branch.
     */
    @Column(name = "branch_id")
    private Long branchId;

    /**
     * Reference to the department this user belongs to.
     * Nullable for users not assigned to a specific department.
     */
    @Column(name = "department_id")
    private Long departmentId;

    // ==============
    // Factory Methods
    // ==============

    /**
     * Factory method to create a new user in PENDING_ACTIVATION status.
     *
     * <p>The application layer is responsible for providing the userId.
     * This ensures proper UUID generation at the application layer.
     *
     * @param userId the unique user identifier (UUID)
     * @param tenantId the tenant this user belongs to
     * @param username the unique username within the tenant
     * @param email the user's email address
     * @param passwordHash the hashed password
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @return a new User instance in PENDING_ACTIVATION status
     */
    public static User create(
            UUID userId,
            Long tenantId,
            String username,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String phoneNumber,
            String jobTitle,
            String profileImageUrl,
            Long branchId,
            Long departmentId) {
        return User.builder()
                .userId(userId)
                .tenantId(tenantId)
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .jobTitle(jobTitle)
                .profileImageUrl(profileImageUrl)
                .branchId(branchId)
                .departmentId(departmentId)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .status(UserStatus.PENDING_ACTIVATION)
                .build();
    }

    // ==============
    // Business Behavior
    // ==============

    /**
     * Activates the user account.
     *
     * <p>Transitions the user from PENDING_ACTIVATION status to ACTIVE.
     * Sets the activatedAt timestamp to track when activation occurred.
     *
     * <p><strong>Business Invariant:</strong> Cannot activate a user that is already active,
     * locked, deactivated, or archived.
     *
     * @param activatedAt the timestamp when activation occurs (provided by application layer)
     * @throws IllegalStateException if the user cannot be activated in its current state
     */
    public void activate(Instant activatedAt) {
        if (this.status == UserStatus.ACTIVE) {
            throw new IllegalStateException("Cannot activate user that is already active: " + this.userId);
        }
        if (this.status == UserStatus.LOCKED) {
            throw new IllegalStateException("Cannot activate locked user: " + this.userId);
        }
        if (this.status == UserStatus.DEACTIVATED) {
            throw new IllegalStateException("Cannot activate deactivated user: " + this.userId);
        }
        if (this.status == UserStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot activate archived user: " + this.userId);
        }

        this.status = UserStatus.ACTIVE;
        this.activatedAt = activatedAt;
        this.lockedAt = null;
        this.lockReason = null;
    }

    /**
     * Locks the user account.
     *
     * <p>Transitions the user from ACTIVE status to LOCKED.
     * Sets the lockedAt timestamp and lockReason to track why the account was locked.
     *
     * <p><strong>Business Invariant:</strong> Cannot lock a user that is not active.
     *
     * @param lockedAt the timestamp when locking occurs (provided by application layer)
     * @param lockReason the reason for locking the account
     * @throws IllegalStateException if the user cannot be locked in its current state
     */
    public void lock(Instant lockedAt, String lockReason) {
        if (this.status != UserStatus.ACTIVE) {
            throw new IllegalStateException("Cannot lock user that is not active: " + this.userId);
        }

        this.status = UserStatus.LOCKED;
        this.lockedAt = lockedAt;
        this.lockReason = lockReason;
    }

    /**
     * Unlocks the user account.
     *
     * <p>Transitions the user from LOCKED status back to ACTIVE.
     * Clears the lockedAt timestamp and lockReason.
     *
     * <p><strong>Business Invariant:</strong> Cannot unlock a user that is not locked.
     *
     * @param unlockedAt the timestamp when unlocking occurs (provided by application layer)
     * @throws IllegalStateException if the user cannot be unlocked in its current state
     */
    public void unlock(Instant unlockedAt) {
        if (this.status != UserStatus.LOCKED) {
            throw new IllegalStateException("Cannot unlock user that is not locked: " + this.userId);
        }

        this.status = UserStatus.ACTIVE;
        this.lockedAt = null;
        this.lockReason = null;
    }

    /**
     * Deactivates the user account.
     *
     * <p>Transitions the user from ACTIVE or LOCKED status to DEACTIVATED.
     * Sets the deactivatedAt timestamp to track when deactivation occurred.
     *
     * <p><strong>Business Invariant:</strong> Cannot deactivate a user that is not active or locked.
     *
     * @param deactivatedAt the timestamp when deactivation occurs (provided by application layer)
     * @throws IllegalStateException if the user cannot be deactivated in its current state
     */
    public void deactivate(Instant deactivatedAt) {
        if (this.status != UserStatus.ACTIVE && this.status != UserStatus.LOCKED) {
            throw new IllegalStateException("Cannot deactivate user that is not active or locked: " + this.userId);
        }

        this.status = UserStatus.DEACTIVATED;
        this.deactivatedAt = deactivatedAt;
    }

    /**
     * Archives the user account.
     *
     * <p>Transitions the user to ARCHIVED status.
     * This is a terminal state - archived users cannot be reactivated.
     * Used for compliance and long-term data retention.
     *
     * @param archivedAt the timestamp when archiving occurs (provided by application layer)
     */
    public void archive(Instant archivedAt) {
        this.status = UserStatus.ARCHIVED;
        this.deactivatedAt = archivedAt;
    }

    /**
     * Updates the last login timestamp.
     *
     * <p>Called after successful authentication to track user activity.
     *
     * @param lastLoginAt the timestamp of the last login
     */
    public void recordLogin(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Updates the stored password hash.
     *
     * <p>The caller (application layer) is responsible for hashing the raw
     * password with BCrypt before invoking this method. Plain-text passwords
     * must never be passed to the domain.
     *
     * @param passwordHash the BCrypt hash of the new password
     */
    public void updatePassword(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        this.passwordHash = passwordHash;
    }

    /**
     * Records a failed login attempt.
     *
     * <p>When the number of consecutive failures reaches {@code maxAttempts},
     * the account is temporarily locked until {@code lockUntil}.
     *
     * @param maxAttempts maximum allowed consecutive failures before lockout
     * @param now         the current instant
     * @param lockUntil   the instant until which the account is locked when the
     *                    threshold is exceeded
     * @return true if this attempt triggered a temporary lockout
     */
    public boolean registerFailedLogin(int maxAttempts, Instant now, Instant lockUntil) {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = lockUntil;
            return true;
        }
        return false;
    }

    /**
     * Resets the consecutive failed-login counter (e.g. after a successful login).
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Checks whether the account is temporarily locked due to failed login attempts.
     *
     * <p>A temporary lock expires automatically once {@code now} passes
     * {@link #lockedUntil}.
     *
     * @param now the current instant
     * @return true if the account is currently temporarily locked
     */
    public boolean isTemporarilyLocked(Instant now) {
        return this.lockedUntil != null && this.lockedUntil.isAfter(now);
    }

    /**
     * Clears a temporary lock (e.g. after a successful password reset).
     */
    public void clearLock() {
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
    }

    /**
     * Updates the user's profile information.
     *
     * @param email the new email address
     * @param firstName the new first name
     * @param lastName the new last name
     */
    public void updateProfile(String email, String firstName, String lastName) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName;
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName;
        }
    }

    /**
     * Updates the user's email address.
     *
     * @param email the new email address
     */
    public void updateEmail(String email) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
    }

    /**
     * Updates the user's first name.
     *
     * @param firstName the new first name
     */
    public void updateFirstName(String firstName) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName;
        }
    }

    /**
     * Updates the user's last name.
     *
     * @param lastName the new last name
     */
    public void updateLastName(String lastName) {
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName;
        }
    }

    /**
     * Updates the user's phone number.
     *
     * @param phoneNumber the new phone number
     */
    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Updates the user's job title.
     *
     * @param jobTitle the new job title
     */
    public void updateJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    /**
     * Updates the user's profile image URL.
     *
     * @param profileImageUrl the new profile image URL
     */
    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * Replaces the stored password hash.
     *
     * <p>Used by the password-reset flow after verifying a valid reset token.
     * The caller is responsible for hashing the raw password (BCrypt) before
     * invoking this method &ndash; the domain never stores plaintext.
     *
     * @param passwordHash the new BCrypt password hash
     */
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Updates the user's branch assignment.
     *
     * @param branchId the new branch ID
     */
    public void updateBranch(Long branchId) {
        this.branchId = branchId;
    }

    /**
     * Updates the user's department assignment.
     *
     * @param departmentId the new department ID
     */
    public void updateDepartment(Long departmentId) {
        this.departmentId = departmentId;
    }

    // ==============
    // Query Methods
    // ==============

    /**
     * Checks if the user is active.
     *
     * @return true if the user status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Checks if the user is locked.
     *
     * @return true if the user status is LOCKED, false otherwise
     */
    public boolean isLocked() {
        return this.status == UserStatus.LOCKED;
    }

    /**
     * Checks if the user is pending activation.
     *
     * @return true if the user status is PENDING_ACTIVATION, false otherwise
     */
    public boolean isPendingActivation() {
        return this.status == UserStatus.PENDING_ACTIVATION;
    }

    /**
     * Checks if the user is deactivated.
     *
     * @return true if the user status is DEACTIVATED, false otherwise
     */
    public boolean isDeactivated() {
        return this.status == UserStatus.DEACTIVATED;
    }

    /**
     * Checks if the user is archived.
     *
     * @return true if the user status is ARCHIVED, false otherwise
     */
    public boolean isArchived() {
        return this.status == UserStatus.ARCHIVED;
    }

    /**
     * Checks if the user can be activated.
     *
     * <p>A user can be activated if it is in PENDING_ACTIVATION status.
     *
     * @return true if the user can be activated, false otherwise
     */
    public boolean canBeActivated() {
        return this.status == UserStatus.PENDING_ACTIVATION;
    }

    /**
     * Checks if the user can be locked.
     *
     * <p>A user can be locked if it is in ACTIVE status.
     *
     * @return true if the user can be locked, false otherwise
     */
    public boolean canBeLocked() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Checks if the user can be unlocked.
     *
     * <p>A user can be unlocked if it is in LOCKED status.
     *
     * @return true if the user can be unlocked, false otherwise
     */
    public boolean canBeUnlocked() {
        return this.status == UserStatus.LOCKED;
    }

    /**
     * Checks if the user can be deactivated.
     *
     * <p>A user can be deactivated if it is in ACTIVE or LOCKED status.
     *
     * @return true if the user can be deactivated, false otherwise
     */
    public boolean canBeDeactivated() {
        return this.status == UserStatus.ACTIVE || this.status == UserStatus.LOCKED;
    }

    /**
     * Gets the full name of the user.
     *
     * @return the full name (first name + last name)
     */
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
}
