package com.erp.business.inventory.items;

import com.erp.business.inventory.categories.CategoryService;
import com.erp.business.inventory.domain.Category;
import com.erp.business.inventory.domain.ExciseType;
import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.domain.ItemType;
import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.business.inventory.domain.Unit;
import com.erp.business.inventory.infrastructure.persistence.CategoryRepository;
import com.erp.business.inventory.infrastructure.persistence.ItemRepository;
import com.erp.business.inventory.infrastructure.persistence.UnitRepository;
import com.erp.business.inventory.items.dto.CreateItemRequest;
import com.erp.business.inventory.items.dto.ItemListResponse;
import com.erp.business.inventory.items.dto.ItemResponse;
import com.erp.business.inventory.categories.exception.CategoryNotFoundException;
import com.erp.business.inventory.items.dto.UpdateItemRequest;
import com.erp.business.inventory.items.exception.DuplicateItemBarcodeException;
import com.erp.business.inventory.items.exception.DuplicateItemSkuException;
import com.erp.business.inventory.items.exception.ItemNotFoundException;
import com.erp.business.inventory.items.exception.InvalidSearchTermException;
import com.erp.business.inventory.items.mapper.ItemMapper;
import com.erp.business.inventory.unit.exception.UnitNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemResponse createItem(Long tenantId, Long companyId, CreateItemRequest request) {
        String formattedSku = request.sku().trim().toUpperCase();
        if (itemRepository.existsByTenantIdAndCompanyIdAndSku(tenantId, companyId, formattedSku)) {
            throw new DuplicateItemSkuException(formattedSku);
        }

        String trimmedBarcode = trimToNull(request.barcode());
        if (trimmedBarcode != null
                && itemRepository.existsByTenantIdAndCompanyIdAndBarcode(tenantId, companyId, trimmedBarcode)) {
            throw new DuplicateItemBarcodeException(trimmedBarcode);
        }

        // Related records are resolved against tenant AND company. Resolving on
        // tenantId alone let a company attach another company's unit or category to
        // its item, because every company in the tenant shares one tenantId.
        Unit primaryUom = unitRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.uomId())
                .orElseThrow(() -> new UnitNotFoundException("Primary UOM not found: " + request.uomId()));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(
                            "Category not found: " + request.categoryId()));
        }

        // sku/nameEn/uom/sellingPrice stay explicit on create; everything else travels as
        // one validated change set so this path and updateItem share the same rules.
        Item item = Item.create(
                tenantId,
                companyId,
                request.sku(),
                request.nameEn(),
                primaryUom,
                request.sellingPrice(),
                Item.ItemChanges.builder()
                        .nameNp(request.nameNp())
                        .hsCode(request.hsCode())
                        .barcode(trimmedBarcode)
                        .descriptionEn(request.descriptionEn())
                        .descriptionNp(request.descriptionNp())
                        .category(category)
                        .itemType(request.itemType())
                        .taxabilityType(request.taxabilityType())
                        .vatRate(request.vatRate())
                        .exciseType(request.exciseType())
                        .exciseRate(request.exciseRate())
                        .exciseAmount(request.exciseAmount())
                        .purchasePrice(request.purchasePrice())
                        .mrp(request.mrp())
                        .isVatInclusive(request.isVatInclusive())
                        .isInventoryTracked(request.isInventoryTracked())
                        .isBatchTracked(request.isBatchTracked())
                        .isExpiryTracked(request.isExpiryTracked())
                        .minStockLevel(request.minStockLevel())
                        .reorderLevel(request.reorderLevel())
                        .reorderQuantity(request.reorderQuantity())
                        .build()
        );

        Item saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long tenantId, Long companyId, Long itemId) {
        Item item = findOrThrow(tenantId, companyId, itemId);
        return itemMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse getItemBySku(Long tenantId, Long companyId, String sku) {
        Item item = itemRepository.findByTenantIdAndCompanyIdAndSku(tenantId, companyId, sku.trim().toUpperCase())
                .orElseThrow(() -> new ItemNotFoundException("Item not found with SKU: " + sku));
        return itemMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse getItemByBarcode(Long tenantId, Long companyId, String barcode) {
        Item item = itemRepository.findByTenantIdAndCompanyIdAndBarcode(tenantId, companyId, barcode.trim())
                .orElseThrow(() -> new ItemNotFoundException("Item not found with barcode: " + barcode));
        return itemMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemListResponse> listItems(Long tenantId, Long companyId, Pageable pageable) {
        return itemRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(itemMapper::toListResponse);
    }

    /**
     * Longest search term accepted. Beyond this the LIKE comparison is doing far more
     * work than any caller needs, and the value could only be a probe.
     */
    private static final int MAX_SEARCH_TERM_LENGTH = 100;

    @Override
    @Transactional(readOnly = true)
    public Page<ItemListResponse> searchItems(Long tenantId, Long companyId, String query, Pageable pageable) {
        return itemRepository.searchItems(tenantId, companyId, normaliseSearchTerm(query), pageable)
                .map(itemMapper::toListResponse);
    }

    /**
     * Trims the term and escapes LIKE metacharacters.
     *
     * <p>The escaping is what stops {@code %} from meaning "match everything". The
     * repository query declares {@code ESCAPE '\'}, so a backslash-prefixed character is
     * matched literally instead of being reinterpreted as a wildcard.
     */
    private static String normaliseSearchTerm(String query) {
        if (query == null || query.isBlank()) {
            throw new InvalidSearchTermException("Search term must not be blank");
        }
        String term = query.trim();
        if (term.length() > MAX_SEARCH_TERM_LENGTH) {
            throw new InvalidSearchTermException(
                    "Search term must not exceed " + MAX_SEARCH_TERM_LENGTH + " characters");
        }
        return term.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemListResponse> listActiveItems(Long tenantId, Long companyId, Pageable pageable) {
        return itemRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(tenantId, companyId, pageable)
                .map(itemMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemListResponse> listItemsByCategory(
            Long tenantId, Long companyId, Long categoryId, Pageable pageable) {
        return itemRepository.findByTenantIdAndCompanyIdAndCategoryId(tenantId, companyId, categoryId, pageable)
                .map(itemMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemListResponse> listItemsByTaxability(
            Long tenantId, Long companyId, TaxabilityType taxabilityType, Pageable pageable) {
        return itemRepository.findByTenantIdAndCompanyIdAndTaxabilityType(tenantId, companyId, taxabilityType, pageable)
                .map(itemMapper::toListResponse);
    }

    @Override
    @Transactional
    public ItemResponse updateItem(Long tenantId, Long companyId, Long itemId, UpdateItemRequest request) {
        Item item = findOrThrow(tenantId, companyId, itemId);

        // Duplicate checks run before the change set is applied so a conflict leaves the
        // entity untouched. Both are scoped to the caller's companyId: findOrThrow proved
        // the item belongs to that company, and keying the checks on the same value keeps
        // them consistent with the load.
        String newSku = request.sku() == null ? null : request.sku().trim().toUpperCase();
        if (newSku != null
                && !newSku.equals(item.getSku())
                && itemRepository.existsByTenantIdAndCompanyIdAndSku(tenantId, companyId, newSku)) {
            throw new DuplicateItemSkuException(newSku);
        }

        String newBarcode = trimToNull(request.barcode());
        if (request.barcode() != null && newBarcode != null
                && !newBarcode.equals(item.getBarcode())
                && itemRepository.existsByTenantIdAndCompanyIdAndBarcode(tenantId, companyId, newBarcode)) {
            throw new DuplicateItemBarcodeException(newBarcode);
        }

        // Related records are resolved against tenant AND company, matching create.
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(
                            "Category not found: " + request.categoryId()));
        }

        Unit uom = null;
        if (request.uomId() != null) {
            uom = unitRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, request.uomId())
                    .orElseThrow(() -> new UnitNotFoundException(
                            "Primary UOM not found: " + request.uomId()));
        }

        item.applyChanges(Item.ItemChanges.builder()
                .sku(newSku)
                .barcode(request.barcode() == null ? null : request.barcode())
                .hsCode(request.hsCode())
                .nameEn(request.nameEn())
                .nameNp(request.nameNp())
                .descriptionEn(request.descriptionEn())
                .descriptionNp(request.descriptionNp())
                .category(category)
                .uom(uom)
                .itemType(request.itemType())
                .taxabilityType(request.taxabilityType())
                .vatRate(request.vatRate())
                .exciseType(request.exciseType())
                .exciseRate(request.exciseRate())
                .exciseAmount(request.exciseAmount())
                .sellingPrice(request.sellingPrice())
                .purchasePrice(request.purchasePrice())
                .mrp(request.mrp())
                .isVatInclusive(request.isVatInclusive())
                .isInventoryTracked(request.isInventoryTracked())
                .isBatchTracked(request.isBatchTracked())
                .isExpiryTracked(request.isExpiryTracked())
                .minStockLevel(request.minStockLevel())
                .reorderLevel(request.reorderLevel())
                .reorderQuantity(request.reorderQuantity())
                .build());

        Item saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ItemResponse activateItem(Long tenantId, Long companyId, Long itemId) {
        Item item = findOrThrow(tenantId, companyId, itemId);
        item.activate();
        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemResponse deactivateItem(Long tenantId, Long companyId, Long itemId) {
        Item item = findOrThrow(tenantId, companyId, itemId);
        item.deactivate();
        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long tenantId, Long companyId, Long itemId) {
        Item item = findOrThrow(tenantId, companyId, itemId);
        itemRepository.delete(item);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Loads an item the caller is allowed to act on.
     *
     * <p>The company predicate is the point: loading on tenant alone meant any
     * authenticated user in the tenant could read, edit, deactivate or delete another
     * company's item purely by guessing its id.
     *
     * <p>A miss reports {@code 404} whether the id belongs to another company or does
     * not exist at all. That is deliberate — distinguishing the two would confirm the
     * existence of a record the caller is not allowed to know about.
     */
    private Item findOrThrow(Long tenantId, Long companyId, Long itemId) {
        return itemRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));
    }
}
