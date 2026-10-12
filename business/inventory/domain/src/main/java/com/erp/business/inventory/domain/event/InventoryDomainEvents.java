package com.erp.business.inventory.domain.event;

import com.erp.platform.events.domain.DomainEvent;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events for Inventory bounded context.
 *
 * <p>These events are published when master data changes, allowing other bounded contexts
 * (Procurement, Sales, etc.) to react and update their denormalized data.
 *
 * @since 1.0.0
 */
public final class InventoryDomainEvents {

    private InventoryDomainEvents() {}

    /**
     * Published when a Unit of Measure is created.
     */
    public static class UnitCreatedEvent implements DomainEvent<UnitCreatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long unitId;
        private final String code;
        private final String name;
        private final String symbol;
        private final String dimension;

        private UnitCreatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                 Map<String, Object> metadata, Long unitId, String code, String name, String symbol, String dimension) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.unitId = unitId;
            this.code = code;
            this.name = name;
            this.symbol = symbol;
            this.dimension = dimension;
        }

        public static UnitCreatedEvent of(String tenantId, Long unitId, String code, String name, String symbol, String dimension) {
            return new UnitCreatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "UnitCreated",
                    Map.of(),
                    unitId, code, name, symbol, dimension
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getUnitId() { return unitId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public String getDimension() { return dimension; }
    }

    /**
     * Published when a Unit of Measure is updated.
     */
    public static class UnitUpdatedEvent implements DomainEvent<UnitUpdatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long unitId;
        private final String code;
        private final String name;
        private final String symbol;
        private final String dimension;
        private final Boolean isActive;

        private UnitUpdatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                 Map<String, Object> metadata, Long unitId, String code, String name, String symbol, String dimension, Boolean isActive) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.unitId = unitId;
            this.code = code;
            this.name = name;
            this.symbol = symbol;
            this.dimension = dimension;
            this.isActive = isActive;
        }

        public static UnitUpdatedEvent of(String tenantId, Long unitId, String code, String name, String symbol, String dimension, Boolean isActive) {
            return new UnitUpdatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "UnitUpdated",
                    Map.of(),
                    unitId, code, name, symbol, dimension, isActive
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getUnitId() { return unitId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public String getDimension() { return dimension; }
        public Boolean getIsActive() { return isActive; }
    }

    /**
     * Published when an Item is created.
     */
    public static class ItemCreatedEvent implements DomainEvent<ItemCreatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long itemId;
        private final String sku;
        private final String name;
        private final Long categoryId;
        private final Long unitId;
        private final String itemType;

        private ItemCreatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                 Map<String, Object> metadata, Long itemId, String sku, String name, Long categoryId, Long unitId, String itemType) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.itemId = itemId;
            this.sku = sku;
            this.name = name;
            this.categoryId = categoryId;
            this.unitId = unitId;
            this.itemType = itemType;
        }

        public static ItemCreatedEvent of(String tenantId, Long itemId, String sku, String name, Long categoryId, Long unitId, String itemType) {
            return new ItemCreatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "ItemCreated",
                    Map.of(),
                    itemId, sku, name, categoryId, unitId, itemType
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getItemId() { return itemId; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public Long getCategoryId() { return categoryId; }
        public Long getUnitId() { return unitId; }
        public String getItemType() { return itemType; }
    }

    /**
     * Published when an Item is updated.
     */
    public static class ItemUpdatedEvent implements DomainEvent<ItemUpdatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long itemId;
        private final String sku;
        private final String name;
        private final Long categoryId;
        private final Long unitId;
        private final String itemType;
        private final Boolean isActive;

        private ItemUpdatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                 Map<String, Object> metadata, Long itemId, String sku, String name, Long categoryId, Long unitId, String itemType, Boolean isActive) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.itemId = itemId;
            this.sku = sku;
            this.name = name;
            this.categoryId = categoryId;
            this.unitId = unitId;
            this.itemType = itemType;
            this.isActive = isActive;
        }

        public static ItemUpdatedEvent of(String tenantId, Long itemId, String sku, String name, Long categoryId, Long unitId, String itemType, Boolean isActive) {
            return new ItemUpdatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "ItemUpdated",
                    Map.of(),
                    itemId, sku, name, categoryId, unitId, itemType, isActive
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getItemId() { return itemId; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public Long getCategoryId() { return categoryId; }
        public Long getUnitId() { return unitId; }
        public String getItemType() { return itemType; }
        public Boolean getIsActive() { return isActive; }
    }

    /**
     * Published when a Warehouse is created.
     */
    public static class WarehouseCreatedEvent implements DomainEvent<WarehouseCreatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long warehouseId;
        private final String code;
        private final String name;
        private final String type;
        private final Long branchId;

        private WarehouseCreatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                      Map<String, Object> metadata, Long warehouseId, String code, String name, String type, Long branchId) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.warehouseId = warehouseId;
            this.code = code;
            this.name = name;
            this.type = type;
            this.branchId = branchId;
        }

        public static WarehouseCreatedEvent of(String tenantId, Long warehouseId, String code, String name, String type, Long branchId) {
            return new WarehouseCreatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "WarehouseCreated",
                    Map.of(),
                    warehouseId, code, name, type, branchId
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getWarehouseId() { return warehouseId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getType() { return type; }
        public Long getBranchId() { return branchId; }
    }

    /**
     * Published when a Warehouse is updated.
     */
    public static class WarehouseUpdatedEvent implements DomainEvent<WarehouseUpdatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long warehouseId;
        private final String code;
        private final String name;
        private final String type;
        private final Long branchId;
        private final Boolean isActive;
        private final Boolean isDefault;

        private WarehouseUpdatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                      Map<String, Object> metadata, Long warehouseId, String code, String name, String type, Long branchId, Boolean isActive, Boolean isDefault) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.warehouseId = warehouseId;
            this.code = code;
            this.name = name;
            this.type = type;
            this.branchId = branchId;
            this.isActive = isActive;
            this.isDefault = isDefault;
        }

        public static WarehouseUpdatedEvent of(String tenantId, Long warehouseId, String code, String name, String type, Long branchId, Boolean isActive, Boolean isDefault) {
            return new WarehouseUpdatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "WarehouseUpdated",
                    Map.of(),
                    warehouseId, code, name, type, branchId, isActive, isDefault
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getWarehouseId() { return warehouseId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getType() { return type; }
        public Long getBranchId() { return branchId; }
        public Boolean getIsActive() { return isActive; }
        public Boolean getIsDefault() { return isDefault; }
    }

    /**
     * Published when a Category is created.
     */
    public static class CategoryCreatedEvent implements DomainEvent<CategoryCreatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long categoryId;
        private final String code;
        private final String name;
        private final String slug;
        private final Long parentId;

        private CategoryCreatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                     Map<String, Object> metadata, Long categoryId, String code, String name, String slug, Long parentId) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.categoryId = categoryId;
            this.code = code;
            this.name = name;
            this.slug = slug;
            this.parentId = parentId;
        }

        public static CategoryCreatedEvent of(String tenantId, Long categoryId, String code, String name, String slug, Long parentId) {
            return new CategoryCreatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "CategoryCreated",
                    Map.of(),
                    categoryId, code, name, slug, parentId
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getCategoryId() { return categoryId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSlug() { return slug; }
        public Long getParentId() { return parentId; }
    }

    /**
     * Published when a Category is updated.
     */
    public static class CategoryUpdatedEvent implements DomainEvent<CategoryUpdatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long categoryId;
        private final String code;
        private final String name;
        private final String slug;
        private final Long parentId;
        private final Boolean isActive;

        private CategoryUpdatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                     Map<String, Object> metadata, Long categoryId, String code, String name, String slug, Long parentId, Boolean isActive) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.categoryId = categoryId;
            this.code = code;
            this.name = name;
            this.slug = slug;
            this.parentId = parentId;
            this.isActive = isActive;
        }

        public static CategoryUpdatedEvent of(String tenantId, Long categoryId, String code, String name, String slug, Long parentId, Boolean isActive) {
            return new CategoryUpdatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "CategoryUpdated",
                    Map.of(),
                    categoryId, code, name, slug, parentId, isActive
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getCategoryId() { return categoryId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSlug() { return slug; }
        public Long getParentId() { return parentId; }
        public Boolean getIsActive() { return isActive; }
    }

    /**
     * Published when a Supplier is created.
     */
    public static class SupplierCreatedEvent implements DomainEvent<SupplierCreatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long supplierId;
        private final String code;
        private final String name;
        private final String supplierType;

        private SupplierCreatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                     Map<String, Object> metadata, Long supplierId, String code, String name, String supplierType) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.supplierId = supplierId;
            this.code = code;
            this.name = name;
            this.supplierType = supplierType;
        }

        public static SupplierCreatedEvent of(String tenantId, Long supplierId, String code, String name, String supplierType) {
            return new SupplierCreatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "SupplierCreated",
                    Map.of(),
                    supplierId, code, name, supplierType
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getSupplierId() { return supplierId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSupplierType() { return supplierType; }
    }

    /**
     * Published when a Supplier is updated.
     */
    public static class SupplierUpdatedEvent implements DomainEvent<SupplierUpdatedEvent>, Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID eventId;
        private final String tenantId;
        private final Instant occurredAt;
        private final String version;
        private final String eventName;
        private final transient Map<String, Object> metadata;
        private final Long supplierId;
        private final String code;
        private final String name;
        private final String supplierType;
        private final Boolean isActive;

        private SupplierUpdatedEvent(UUID eventId, String tenantId, Instant occurredAt, String version, String eventName,
                                     Map<String, Object> metadata, Long supplierId, String code, String name, String supplierType, Boolean isActive) {
            this.eventId = eventId;
            this.tenantId = tenantId;
            this.occurredAt = occurredAt;
            this.version = version;
            this.eventName = eventName;
            this.metadata = metadata;
            this.supplierId = supplierId;
            this.code = code;
            this.name = name;
            this.supplierType = supplierType;
            this.isActive = isActive;
        }

        public static SupplierUpdatedEvent of(String tenantId, Long supplierId, String code, String name, String supplierType, Boolean isActive) {
            return new SupplierUpdatedEvent(
                    UUID.randomUUID(),
                    tenantId,
                    Instant.now(),
                    "1.0",
                    "SupplierUpdated",
                    Map.of(),
                    supplierId, code, name, supplierType, isActive
            );
        }

        @Override public UUID getEventId() { return eventId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getVersion() { return version; }
        @Override public String getEventName() { return eventName; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
        public Long getSupplierId() { return supplierId; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getSupplierType() { return supplierType; }
        public Boolean getIsActive() { return isActive; }
    }
}
