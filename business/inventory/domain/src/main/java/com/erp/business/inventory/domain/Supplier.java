package com.erp.business.inventory.domain;

import com.erp.business.inventory.shared.domain.TenantScopedEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "suppliers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_supplier_code",
                        columnNames = {"company_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_company_supplier_name",
                        columnNames = {"company_id", "name"}
                )
        },
        indexes = {
                @Index(name = "idx_suppliers_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_suppliers_type", columnList = "tenant_id, company_id, type")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Supplier extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private SupplierType type;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_id", length = 64)
    private String taxId;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Column(name = "currency", length = 8)
    private String currency;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public static Supplier create(Long tenantId, Long companiesId, String name, String code,
                                  SupplierType type, String email, String phone, String address,
                                  String taxId, Integer paymentTermsDays, String currency) {
        validateTenantId(tenantId);
        validateCompanyId(companiesId);
        validateName(name);
        validateCode(code);

        Supplier supplier = Supplier.builder()
                .name(name.trim())
                .code(code.trim().toUpperCase())
                .type(type)
                .email(trimToNull(email))
                .phone(trimToNull(phone))
                .address(trimToNull(address))
                .taxId(trimToNull(taxId))
                .paymentTermsDays(paymentTermsDays)
                .currency(currency != null ? currency.trim().toUpperCase() : "NPR")
                .isActive(true)
                .build();

        supplier.assignScope(tenantId, companiesId);
        return supplier;
    }

    public void update(String name, String code, SupplierType type, String email, String phone,
                       String address, String taxId, Integer paymentTermsDays, String currency) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (code != null) {
            validateCode(code);
            this.code = code.trim().toUpperCase();
        }
        if (type != null) {
            this.type = type;
        }
        this.email = trimToNull(email);
        this.phone = trimToNull(phone);
        this.address = trimToNull(address);
        this.taxId = trimToNull(taxId);
        this.paymentTermsDays = paymentTermsDays;
        if (currency != null) {
            this.currency = currency.trim().toUpperCase();
        }
    }

    private static void validateTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be a positive number");
        }
    }

    private static void validateCompanyId(Long companiesId) {
        if (companiesId == null || companiesId <= 0) {
            throw new IllegalArgumentException("Company ID must be a positive number");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Supplier name must not be blank");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Supplier name must not exceed 255 characters");
        }
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Supplier code must not be blank");
        }
        if (code.length() > 64) {
            throw new IllegalArgumentException("Supplier code must not exceed 64 characters");
        }
    }

    private static String trimToNull(String value) {
        return value == null ? null : (value.trim().isEmpty() ? null : value.trim());
    }
}
