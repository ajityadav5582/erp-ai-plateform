package com.erp.business.inventory.interfaces.rest;

import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.business.inventory.items.ItemService;
import com.erp.business.inventory.items.dto.CreateItemRequest;
import com.erp.business.inventory.items.dto.ItemListResponse;
import com.erp.business.inventory.items.dto.ItemResponse;
import com.erp.business.inventory.items.dto.UpdateItemRequest;
import com.erp.business.inventory.shared.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(
            @Valid @RequestBody CreateItemRequest request,
            UriComponentsBuilder uriBuilder) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.createItem(tenantId, companyId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/inventory/items/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ItemListResponse>> listItems(Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<ItemListResponse> page = itemService.listItems(tenantId, companyId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ItemListResponse>> searchItems(
            @RequestParam String query,
            Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<ItemListResponse> page = itemService.searchItems(tenantId, companyId, query, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/active")
    public ResponseEntity<Page<ItemListResponse>> listActiveItems(Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<ItemListResponse> page = itemService.listActiveItems(tenantId, companyId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<Page<ItemListResponse>> listItemsByCategory(
            @PathVariable Long categoryId,
            Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<ItemListResponse> page = itemService.listItemsByCategory(tenantId, companyId, categoryId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/by-taxability/{taxabilityType}")
    public ResponseEntity<Page<ItemListResponse>> listItemsByTaxability(
            @PathVariable TaxabilityType taxabilityType,
            Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        Page<ItemListResponse> page = itemService.listItemsByTaxability(tenantId, companyId, taxabilityType, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/by-sku/{sku}")
    public ResponseEntity<ItemResponse> getItemBySku(@PathVariable String sku) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.getItemBySku(tenantId, companyId, sku);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-barcode/{barcode}")
    public ResponseEntity<ItemResponse> getItemByBarcode(@PathVariable String barcode) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.getItemByBarcode(tenantId, companyId, barcode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long itemId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.getItemById(tenantId, companyId, itemId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.updateItem(tenantId, companyId, itemId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemResponse> patchItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.updateItem(tenantId, companyId, itemId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{itemId}/activate")
    public ResponseEntity<ItemResponse> activateItem(@PathVariable Long itemId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.activateItem(tenantId, companyId, itemId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{itemId}/deactivate")
    public ResponseEntity<ItemResponse> deactivateItem(@PathVariable Long itemId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        ItemResponse response = itemService.deactivateItem(tenantId, companyId, itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        itemService.deleteItem(tenantId, companyId, itemId);
        return ResponseEntity.noContent().build();
    }
}
