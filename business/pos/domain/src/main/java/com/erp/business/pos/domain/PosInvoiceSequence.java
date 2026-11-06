package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pos_invoice_sequence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PosInvoiceSequence extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @Column(name = "fiscal_year", nullable = false, length = 16)
    private String fiscalYear;

    @Column(name = "prefix", nullable = false, length = 16)
    private String prefix;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public Long nextNumber() {
        this.lastNumber = (this.lastNumber == null ? 0L : this.lastNumber) + 1;
        return this.lastNumber;
    }
}
