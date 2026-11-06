package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

/**
 * Per-company POS operational configuration.
 *
 * <p>One row per {@code (tenant, company)}. Holds the shared POS settings that
 * vary by company (enabled payment methods, cash rounding, service charge,
 * tip handling, default register/warehouse, price list) plus a free-form
 * {@link #restaurantSettings} JSON blob for restaurant-specific options
 * (table count, kitchen printer, course config). Restaurant-only data lives
 * here as configuration; restaurant runtime entities (Table, KitchenOrder, ...)
 * live in the {@code business/pos/restaurant} module.
 *
 * <p>Shared across all business types — retail companies simply leave the
 * restaurant settings null.
 *
 * @since 1.0.0
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "pos_company_configs",
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(
                        name = "uk_pos_company_configs_tenant_company",
                        columnNames = {"tenant_id", "company_id"}
                )
        },
        indexes = {
                @Index(name = "idx_pos_company_configs_tenant_company",
                        columnList = "tenant_id, company_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PosCompanyConfig extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Comma-separated list of enabled {@link PosPaymentMethod} names.
     * Null/blank means "all methods enabled" (backward-compatible default).
     */
    @Size(max = 500)
    @Column(name = "enabled_payment_methods", length = 500)
    private String enabledPaymentMethods;

    @Column(name = "cash_rounding", nullable = false,
            columnDefinition = "boolean default true")
    @Builder.Default
    private boolean cashRounding = true;

    @Column(name = "service_charge_percent", precision = 5, scale = 2,
            columnDefinition = "numeric(5,2) default 0")
    @Builder.Default
    private BigDecimal serviceChargePercent = BigDecimal.ZERO;

    /**
     * How tips are handled: {@code NONE}, {@code ADD_TO_BILL}, {@code SEPARATE}.
     */
    @Size(max = 20)
    @Column(name = "tip_handling", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'NONE'")
    @Builder.Default
    private String tipHandling = "NONE";

    @Column(name = "default_register_id")
    private Long defaultRegisterId;

    @Column(name = "default_warehouse_id")
    private Long defaultWarehouseId;

    @Column(name = "price_list_id")
    private Long priceListId;

    /**
     * Free-form JSON for restaurant-specific settings (table count, kitchen
     * printer, course config). Null for non-restaurant companies.
     */
    @Column(name = "restaurant_settings", columnDefinition = "TEXT")
    private String restaurantSettings;

    // -----------------------------------------------------------------------
    // Domain helpers
    // -----------------------------------------------------------------------

    public void updateEnabledPaymentMethods(String methods) {
        this.enabledPaymentMethods = (methods == null || methods.isBlank()) ? null : methods.trim();
    }

    public void updateCashRounding(boolean cashRounding) {
        this.cashRounding = cashRounding;
    }

    public void updateServiceChargePercent(BigDecimal percent) {
        if (percent != null) {
            this.serviceChargePercent = percent;
        }
    }

    public void updateTipHandling(String tipHandling) {
        if (tipHandling != null && !tipHandling.isBlank()) {
            this.tipHandling = tipHandling.trim().toUpperCase();
        }
    }

    public void updateDefaultRegisterId(Long registerId) {
        this.defaultRegisterId = registerId;
    }

    public void updateDefaultWarehouseId(Long warehouseId) {
        this.defaultWarehouseId = warehouseId;
    }

    public void updatePriceListId(Long priceListId) {
        this.priceListId = priceListId;
    }

    public void updateRestaurantSettings(String settings) {
        this.restaurantSettings = (settings == null || settings.isBlank()) ? null : settings.trim();
    }
}
