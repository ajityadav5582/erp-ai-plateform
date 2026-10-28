package com.erp.business.pos.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(
        name = "pos_tables",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pos_tables_tenant_company_name",
                columnNames = {"tenant_id", "company_id", "table_name"}
        ),
        indexes = {
                @Index(name = "idx_pos_tables_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_pos_tables_branch", columnList = "tenant_id, company_id, branch_id"),
                @Index(name = "idx_pos_tables_status", columnList = "tenant_id, company_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PosTable extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 100)
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "branch_id")
    private Long branchId;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "AVAILABLE";

    public static PosTable create(String tableName, Long branchId) {
        PosTable table = PosTable.builder()
                .tableName(tableName.trim())
                .branchId(branchId)
                .status("AVAILABLE")
                .build();
        return table;
    }

    public void markOccupied() {
        this.status = "OCCUPIED";
    }

    public void markAvailable() {
        this.status = "AVAILABLE";
    }
}
