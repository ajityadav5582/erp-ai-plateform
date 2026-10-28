package com.erp.business.pos.table;

import com.erp.business.pos.table.dto.PosTableResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/tables")
@RequiredArgsConstructor
public class PosTableController {

    private final PosTableService tableService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<PosTableResponse> createTable(@RequestParam String tableName,
                                                        @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(tableService.createTable(
                context.getTenantId(), context.getCompanyId(), tableName, branchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosTableResponse> getTable(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.getTableById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @GetMapping
    public ResponseEntity<Page<PosTableResponse>> listTables(Pageable pageable) {
        return ResponseEntity.ok(tableService.listTables(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/available")
    public ResponseEntity<List<PosTableResponse>> listAvailable() {
        return ResponseEntity.ok(tableService.listAvailableTables(
                context.getTenantId(), context.getCompanyId()));
    }

    @PostMapping("/{id}/occupy")
    public ResponseEntity<PosTableResponse> occupy(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.markOccupied(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<PosTableResponse> release(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.markAvailable(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
