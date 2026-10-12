package com.erp.business.sales.customer.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Customer master: the party the company sells to.
 *
 * <p>The sell-side counterpart of a supplier. It is referenced by id from the
 * sales order, delivery and invoice aggregates, with a frozen snapshot of the
 * display fields copied onto each document so a later rename here never rewrites
 * history on an issued tax invoice.
 *
 * <p><strong>IRD fields.</strong> {@code panNumber} is the customer's Permanent
 * Account Number and {@code vatRegistered} says whether the customer is a
 * VAT-registered person. A tax invoice issued to a VAT-registered buyer must
 * carry that buyer's PAN, so the aggregate enforces the invariant that a
 * VAT-registered customer always has a PAN - see {@link #validateVatPan()}.
 *
 * <p><strong>Invariants live here, not in the service.</strong> Name, code and
 * the PAN/VAT rule are validated inside the factory and mutator, so no caller can
 * construct a customer that violates them regardless of how the service evolves.
 */
@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customers_company_code",
                        columnNames = {"tenant_id", "company_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_customers_company_name",
                        columnNames = {"tenant_id", "company_id", "name"}
                )
        },
        indexes = {
                @Index(name = "idx_customers_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_customers_type", columnList = "tenant_id, company_id, type"),
                @Index(name = "idx_customers_pan", columnList = "tenant_id, company_id, pan_number")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Customer extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Nepal IRD Permanent Account Number: exactly nine digits. Null for a
     * non-registered (B2C) customer, which is legitimate.
     */
    private static final Pattern PAN_PATTERN = Pattern.compile("\\d{9}");

    private static final int MONEY_SCALE = 4;

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotBlank(message = "Customer code is required")
    @Size(max = 64, message = "Customer code must not exceed 64 characters")
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @NotNull(message = "Customer type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private CustomerType type;

    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", length = 255)
    private String email;

    @Size(max = 32, message = "Phone must not exceed 32 characters")
    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * Nepal IRD Permanent Account Number (9 digits). Required when
     * {@link #vatRegistered} is true; null otherwise.
     */
    @Column(name = "pan_number", length = 16)
    private String panNumber;

    /**
     * Whether the customer is a VAT-registered person. Drives whether a tax
     * invoice must show the buyer's PAN and whether the sale is a B2B supply.
     */
    @NotNull(message = "VAT registration flag is required")
    @Column(name = "vat_registered", nullable = false)
    @Builder.Default
    private Boolean vatRegistered = false;

    /** Credit ceiling for receivables. Null means no limit is enforced here. */
    @PositiveOrZero(message = "Credit limit must not be negative")
    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Size(max = 8, message = "Currency must not exceed 8 characters")
    @Column(name = "currency", length = 8)
    @Builder.Default
    private String currency = "NPR";

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // -----------------------------------------------------------------------
    // Behaviour
    // -----------------------------------------------------------------------

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Creates a new customer. All invariants are enforced here so an invalid
     * customer can never be built, whatever the calling service does.
     *
     * @param tenantId         owning tenant, from the signed token
     * @param companyId        owning company, from the request header
     * @param name             display name, required
     * @param code             short unique code, required
     * @param type             business classification, required
     * @param email            contact email, optional
     * @param phone            contact phone, optional
     * @param address          billing address, optional
     * @param panNumber        IRD PAN (9 digits), required when {@code vatRegistered}
     * @param vatRegistered    whether the customer is VAT-registered
     * @param creditLimit      receivables ceiling, optional
     * @param paymentTermsDays agreed payment terms, optional
     * @param currency         settlement currency, defaults to NPR
     * @return a new unsaved customer
     */
    public static Customer create(Long tenantId, Long companyId, String name, String code,
                                  CustomerType type, String email, String phone, String address,
                                  String panNumber, boolean vatRegistered, BigDecimal creditLimit,
                                  Integer paymentTermsDays, String currency) {
        Customer customer = Customer.builder()
                .name(validateName(name))
                .code(validateCode(code))
                .type(validateType(type))
                .email(trimToNull(email))
                .phone(trimToNull(phone))
                .address(trimToNull(address))
                .panNumber(validatePan(panNumber))
                .vatRegistered(vatRegistered)
                .creditLimit(validateCreditLimit(creditLimit))
                .paymentTermsDays(paymentTermsDays)
                .currency(currency != null && !currency.isBlank() ? currency.trim().toUpperCase() : "NPR")
                .isActive(true)
                .build();

        customer.validateVatPan();
        customer.assignScope(tenantId, companyId);
        return customer;
    }

    /**
     * Updates the mutable fields. Null name/code/type leave the current value
     * unchanged; the remaining fields are replaced wholesale (a null clears an
     * optional value), matching how a master-data edit form submits the whole row.
     */
    public void update(String name, String code, CustomerType type, String email, String phone,
                       String address, String panNumber, boolean vatRegistered,
                       BigDecimal creditLimit, Integer paymentTermsDays, String currency) {
        if (name != null) {
            this.name = validateName(name);
        }
        if (code != null) {
            this.code = validateCode(code);
        }
        if (type != null) {
            this.type = validateType(type);
        }
        this.email = trimToNull(email);
        this.phone = trimToNull(phone);
        this.address = trimToNull(address);
        this.panNumber = validatePan(panNumber);
        this.vatRegistered = vatRegistered;
        this.creditLimit = validateCreditLimit(creditLimit);
        this.paymentTermsDays = paymentTermsDays;
        if (currency != null && !currency.isBlank()) {
            this.currency = currency.trim().toUpperCase();
        }
        validateVatPan();
    }

    /**
     * The IRD invariant: a VAT-registered customer must carry a PAN, because a
     * tax invoice to that buyer has to print it. Enforced after every mutation.
     */
    private void validateVatPan() {
        if (Boolean.TRUE.equals(vatRegistered) && (panNumber == null || panNumber.isBlank())) {
            throw new IllegalStateException(
                    "A VAT-registered customer must have a PAN number for IRD tax invoices");
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Customer name must not exceed 255 characters");
        }
        return name.trim();
    }

    private static String validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Customer code must not be blank");
        }
        if (code.length() > 64) {
            throw new IllegalArgumentException("Customer code must not exceed 64 characters");
        }
        return code.trim().toUpperCase();
    }

    private static CustomerType validateType(CustomerType type) {
        if (type == null) {
            throw new IllegalArgumentException("Customer type is required");
        }
        return type;
    }

    private static String validatePan(String panNumber) {
        String trimmed = trimToNull(panNumber);
        if (trimmed == null) {
            return null;
        }
        if (!PAN_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("PAN number must be exactly 9 digits");
        }
        return trimmed;
    }

    private static BigDecimal validateCreditLimit(BigDecimal creditLimit) {
        if (creditLimit == null) {
            return null;
        }
        if (creditLimit.signum() < 0) {
            throw new IllegalArgumentException("Credit limit must not be negative");
        }
        return creditLimit.setScale(MONEY_SCALE, java.math.RoundingMode.HALF_UP);
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
