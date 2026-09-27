package com.erp.platform.identity.domain;

import com.erp.platform.data.lock.OptimisticLock;
import jakarta.persistence.*;
import lombok.*;

/**
 * Business action (operation) master data aggregate.
 *
 * <p>Represents an operation type that can be performed on resources
 * (e.g., CREATE, VIEW, UPDATE, DELETE). Actions categorize permissions
 * across the platform and drive dynamic UI permission matrices.
 *
 * <p>Actions are master data shared across tenants and are therefore not tenant-scoped.
 *
 * <p>Architectural Directives Enforced:
 * <ul>
 *   <li>Extends {@link OptimisticLock} for optimistic locking support</li>
 *   <li>Strict Lombok scoping: PROTECTED no-args constructor, PRIVATE all-args constructor</li>
 *   <li>Dynamic UI metadata support: {@code category} and {@code displayOrder}</li>
 *   <li>Compile-time to runtime bridge via {@link #fromEnum(Action, boolean)} factory method</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "actions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_actions_action_code", columnNames = {"action_code"})
        },
        indexes = {
                @Index(name = "idx_actions_category_order", columnList = "category, display_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class ActionEntity extends OptimisticLock<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Unique action code (e.g., CREATE, VIEW, UPDATE, DELETE).
     */
    @Column(name = "action_code", nullable = false, length = 50)
    private String actionCode;

    /**
     * Human-readable name of the action.
     */
    @Column(name = "action_name", nullable = false, length = 100)
    private String actionName;

    /**
     * Detailed description of the action's purpose.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Category of the action (CRUD, WORKFLOW, DATA_IO, ADMIN) for UI matrix layout.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ActionCategory category;

    /**
     * Display order for rendering action headers in frontend grids.
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Current status of the action.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActionStatus status;

    /**
     * Indicates if this is a system-level action that cannot be deleted or modified by tenants.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    // ==================== Factory Methods ====================

    /**
     * Creates an {@link ActionEntity} instance from a compile-time {@link Action} enum.
     *
     * <p>Serves as the factory bridge between compile-time security definitions
     * and runtime database master records.
     *
     * @param action the compile-time Action enum
     * @param isSystem true if this action is system-managed
     * @return a fully populated ActionEntity
     */
    public static ActionEntity fromEnum(Action action, boolean isSystem) {
        if (action == null) {
            throw new IllegalArgumentException("Action enum must not be null");
        }

        return ActionEntity.builder()
                .actionCode(action.getCode())
                .actionName(action.getDefaultName())
                .description(action.getDescription())
                .category(action.getCategory())
                .displayOrder(action.getDisplayOrder())
                .status(ActionStatus.ACTIVE)
                .isSystem(isSystem)
                .build();
    }

    /**
     * Explicit factory method to create an ActionEntity manually.
     */
    public static ActionEntity create(
            String actionCode,
            String actionName,
            String description,
            ActionCategory category,
            Integer displayOrder,
            ActionStatus status,
            Boolean isSystem) {

        validateActionCode(actionCode);
        validateActionName(actionName);

        return ActionEntity.builder()
                .actionCode(actionCode.trim().toUpperCase())
                .actionName(actionName.trim())
                .description(description != null ? description.trim() : null)
                .category(category != null ? category : ActionCategory.CRUD)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .status(status != null ? status : ActionStatus.ACTIVE)
                .isSystem(isSystem != null ? isSystem : false)
                .build();
    }

    /**
     * Overloaded backward-compatible factory method.
     */
    public static ActionEntity create(String actionCode, String actionName, String description, ActionStatus status, Boolean isSystem) {
        Action actionEnum = Action.fromCode(actionCode).orElse(null);
        ActionCategory category = actionEnum != null ? actionEnum.getCategory() : ActionCategory.CRUD;
        Integer displayOrder = actionEnum != null ? actionEnum.getDisplayOrder() : 0;
        return create(actionCode, actionName, description, category, displayOrder, status, isSystem);
    }

    // ==================== Domain Behavior Methods ====================

    /**
     * Activates the action entity.
     */
    public void activate() {
        if (this.status == ActionStatus.ACTIVE) {
            throw new IllegalStateException("Action is already active");
        }
        this.status = ActionStatus.ACTIVE;
    }

    /**
     * Deactivates the action entity.
     */
    public void deactivate() {
        if (this.status != ActionStatus.ACTIVE) {
            throw new IllegalStateException("Only active actions can be deactivated");
        }
        this.status = ActionStatus.INACTIVE;
    }

    // ==================== Validation Methods ====================

    private static void validateActionCode(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            throw new IllegalArgumentException("Action code must not be blank");
        }
        if (actionCode.length() > 50) {
            throw new IllegalArgumentException("Action code must not exceed 50 characters");
        }
    }

    private static void validateActionName(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            throw new IllegalArgumentException("Action name must not be blank");
        }
        if (actionName.length() > 100) {
            throw new IllegalArgumentException("Action name must not exceed 100 characters");
        }
    }
}
