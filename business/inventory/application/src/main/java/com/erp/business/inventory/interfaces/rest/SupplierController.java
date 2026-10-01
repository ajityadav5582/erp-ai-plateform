package com.erp.business.inventory.interfaces.rest;

import com.erp.business.inventory.shared.context.BusinessContextAccessor;
import com.erp.business.inventory.supplier.dto.SupplierCreateRequest;
import com.erp.business.inventory.supplier.dto.SupplierResponse;
import com.erp.business.inventory.supplier.dto.SupplierUpdateRequest;
import com.erp.business.inventory.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/inventory/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(
            @Valid @RequestBody SupplierCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        SupplierResponse response = supplierService.createSupplier(tenantId, companyId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/inventory/suppliers/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SupplierResponse>> listSuppliers(Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<SupplierResponse> page = supplierService.listSuppliers(tenantId, companyId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        SupplierResponse response = supplierService.getSupplierById(tenantId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-code")
    public ResponseEntity<SupplierResponse> getSupplierByCode(@RequestParam String code) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        SupplierResponse response = supplierService.getSupplierByCode(tenantId, companyId, code);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierUpdateRequest request) {
        Long tenantId = context.getTenantId();
        SupplierResponse response = supplierService.updateSupplier(tenantId, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SupplierResponse> patchSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierUpdateRequest request) {
        Long tenantId = context.getTenantId();
        SupplierResponse response = supplierService.updateSupplier(tenantId, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<SupplierResponse> activateSupplier(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        SupplierResponse response = supplierService.activateSupplier(tenantId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<SupplierResponse> deactivateSupplier(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        SupplierResponse response = supplierService.deactivateSupplier(tenantId, id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        Long tenantId = context.getTenantId();
        supplierService.deleteSupplier(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
