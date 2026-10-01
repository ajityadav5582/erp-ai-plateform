package com.erp.business.inventory.items;

import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.business.inventory.items.dto.CreateItemRequest;
import com.erp.business.inventory.items.dto.ItemListResponse;
import com.erp.business.inventory.items.dto.ItemResponse;
import com.erp.business.inventory.items.dto.UpdateItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemService {

    ItemResponse createItem(Long tenantId, Long companyId, CreateItemRequest request);

    ItemResponse getItemById(Long tenantId, Long companyId, Long itemId);

    ItemResponse getItemBySku(Long tenantId, Long companyId, String sku);

    ItemResponse getItemByBarcode(Long tenantId, Long companyId, String barcode);

    Page<ItemListResponse> listItems(Long tenantId, Long companyId, Pageable pageable);

    Page<ItemListResponse> searchItems(Long tenantId, Long companyId, String query, Pageable pageable);

    Page<ItemListResponse> listActiveItems(Long tenantId, Long companyId, Pageable pageable);

    Page<ItemListResponse> listItemsByCategory(Long tenantId, Long companyId, Long categoryId, Pageable pageable);

    Page<ItemListResponse> listItemsByTaxability(Long tenantId, Long companyId, TaxabilityType taxabilityType, Pageable pageable);

    ItemResponse updateItem(Long tenantId, Long companyId, Long itemId, UpdateItemRequest request);

    ItemResponse activateItem(Long tenantId, Long companyId, Long itemId);

    ItemResponse deactivateItem(Long tenantId, Long companyId, Long itemId);

    void deleteItem(Long tenantId, Long companyId, Long itemId);
}
