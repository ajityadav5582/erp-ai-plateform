package com.erp.business.pos.quickitem;

import java.math.BigDecimal;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.pos.domain.PosQuickItem;
import com.erp.business.pos.quickitem.dto.BatchLayoutRequest;
import com.erp.business.pos.quickitem.dto.CreateQuickItemRequest;
import com.erp.business.pos.quickitem.dto.PosQuickItemResolvedDto;
import com.erp.business.pos.quickitem.dto.PosQuickItemResponse;
import com.erp.business.pos.quickitem.dto.ReorderQuickItemsRequest;
import com.erp.business.pos.quickitem.dto.UpdateQuickItemRequest;
import com.erp.business.pos.quickitem.exception.PosQuickItemNotFoundException;
import com.erp.business.pos.quickitem.infrastructure.persistence.PosQuickItemRepository;
import com.erp.business.pos.quickitem.mapper.PosQuickItemMapper;
import com.erp.business.pos.shared.security.PosQuickItemPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * POS quick-item use-case implementation.
 *
 * <p>Quick items are terminal configuration, not transactional records: the
 * item snapshot (code, name, UOM) is resolved from the Inventory catalogue on
 * every create and update, so a tile can never carry a stale or hand-edited
 * snapshot, and removal is a hard delete. The grid load is a single query that
 * returns the company-shared tiles plus one register's tiles, so the terminal
 * renders the whole grid in one round-trip.
 *
 * <p>Authorisation is declared per method with {@code @RequiresPermission} and
 * enforced by the shared aspect before the body runs.
 */
@Service
@Transactional
public class PosQuickItemServiceImpl implements PosQuickItemService {

    private final PosQuickItemRepository quickItemRepository;
    private final PosQuickItemMapper quickItemMapper;
    private final ItemService itemService;

    public PosQuickItemServiceImpl(PosQuickItemRepository quickItemRepository,
                                   PosQuickItemMapper quickItemMapper,
                                   ItemService itemService) {
        this.quickItemRepository = quickItemRepository;
        this.quickItemMapper = quickItemMapper;
        this.itemService = itemService;
    }

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @Override
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_CREATE)
    public PosQuickItemResponse createQuickItem(Long tenantId, Long companyId,
                                                CreateQuickItemRequest request) {
        ItemService.ItemResponse item = resolveItem(request.itemId());

        PosQuickItem quickItem = PosQuickItem.create(
                request.registerId(), item.getId(), item.getSku(), item.getName(), item.getUnitCode(),
                request.defaultPrice(), request.gridRow(), request.gridCol(),
                request.displayColor(), request.displayIcon(), request.sortOrder());
        quickItem.assignScope(tenantId, companyId);

        return quickItemMapper.toResponse(quickItemRepository.save(quickItem));
    }

    @Override
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_UPDATE)
    public PosQuickItemResponse updateQuickItem(Long tenantId, Long companyId, Long id,
                                                UpdateQuickItemRequest request) {
        PosQuickItem quickItem = load(tenantId, companyId, id);
        ItemService.ItemResponse item = resolveItem(request.itemId());

        quickItem.updateDetails(
                item.getId(), item.getSku(), item.getName(), item.getUnitCode(),
                request.defaultPrice(), request.gridRow(), request.gridCol(),
                request.displayColor(), request.displayIcon(), request.sortOrder());

        return quickItemMapper.toResponse(quickItemRepository.save(quickItem));
    }

    @Override
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_UPDATE)
    public PosQuickItemResponse activateQuickItem(Long tenantId, Long companyId, Long id) {
        PosQuickItem quickItem = load(tenantId, companyId, id);
        quickItem.activate();
        return quickItemMapper.toResponse(quickItemRepository.save(quickItem));
    }

    @Override
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_UPDATE)
    public PosQuickItemResponse deactivateQuickItem(Long tenantId, Long companyId, Long id) {
        PosQuickItem quickItem = load(tenantId, companyId, id);
        quickItem.deactivate();
        return quickItemMapper.toResponse(quickItemRepository.save(quickItem));
    }

    @Override
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_DELETE)
    public void deleteQuickItem(Long tenantId, Long companyId, Long id) {
        PosQuickItem quickItem = load(tenantId, companyId, id);
        quickItemRepository.delete(quickItem);
    }

    @Override
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_UPDATE)
    public List<PosQuickItemResponse> reorderQuickItems(Long tenantId, Long companyId,
                                                        ReorderQuickItemsRequest request) {
        List<PosQuickItem> tiles = request.placements().stream()
                .map(placement -> {
                    PosQuickItem quickItem = load(tenantId, companyId, placement.id());
                    quickItem.reposition(placement.gridRow(), placement.gridCol(), placement.sortOrder());
                    return quickItem;
                })
                .toList();

        return quickItemRepository.saveAll(tiles).stream()
                .map(quickItemMapper::toResponse)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_VIEW)
    public PosQuickItemResponse getQuickItemById(Long tenantId, Long companyId, Long id) {
        return quickItemMapper.toResponse(load(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_VIEW)
    public List<PosQuickItemResponse> loadGrid(Long tenantId, Long companyId, Long registerId) {
        return quickItemRepository.findGridForRegister(tenantId, companyId, registerId).stream()
                .map(quickItemMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_VIEW)
    public Page<PosQuickItemResponse> listQuickItems(Long tenantId, Long companyId, Pageable pageable) {
        return quickItemRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(quickItemMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_VIEW)
    public Page<PosQuickItemResponse> listQuickItemsByRegister(Long tenantId, Long companyId,
                                                               Long registerId, Pageable pageable) {
        return quickItemRepository.findByTenantIdAndCompanyIdAndRegisterId(tenantId, companyId, registerId, pageable)
                .map(quickItemMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_VIEW)
    public Page<PosQuickItemResponse> listCompanySharedQuickItems(Long tenantId, Long companyId,
                                                                  Pageable pageable) {
        return quickItemRepository.findByTenantIdAndCompanyIdAndRegisterIdIsNull(tenantId, companyId, pageable)
                .map(quickItemMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_VIEW)
    public Page<PosQuickItemResponse> searchQuickItems(Long tenantId, Long companyId, String term,
                                                       Pageable pageable) {
        return quickItemRepository.search(tenantId, companyId, term, pageable)
                .map(quickItemMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_VIEW)
    public List<PosQuickItemResolvedDto> resolveTilesForTerminal(Long tenantId, Long companyId,
                                                                  Long registerId, String tabName,
                                                                  Integer pageNumber) {
        List<PosQuickItem> tiles = quickItemRepository.findTilesForTerminal(
                tenantId, companyId, tabName, pageNumber, registerId);

        // Override: register-specific tile replaces company-default at same coordinate.
        java.util.Map<String, PosQuickItem> bySlot = new java.util.HashMap<>();
        for (PosQuickItem t : tiles) {
            String key = t.getGridRow() + "-" + t.getGridCol();
            PosQuickItem existing = bySlot.get(key);
            if (existing == null || (existing.getRegisterId() == null && t.getRegisterId() != null)) {
                bySlot.put(key, t);
            }
        }

        List<PosQuickItemResolvedDto> result = new java.util.ArrayList<>();
        for (PosQuickItem t : bySlot.values()) {
            BigDecimal effectivePrice = t.getDefaultPrice();
            BigDecimal vatRate = null;
            if (effectivePrice == null && t.getItemId() != null) {
                ItemService.ItemResponse item = itemService.findById(t.getItemId()).orElse(null);
                if (item != null) {
                    effectivePrice = item.getSellingPrice();
                    vatRate = item.getVatRate();
                }
            }
            String scope = (t.getRegisterId() == null) ? "COMPANY" : "REGISTER";
            result.add(new PosQuickItemResolvedDto(
                    t.getGridRow(),
                    t.getGridCol(),
                    scope,
                    t.getId(),
                    t.getItemType(),
                    t.getItemId(),
                    t.getItemCode(),
                    t.getItemName(),
                    t.getUomCode(),
                    effectivePrice,
                    vatRate,
                    t.isTaxInclusive(),
                    t.getHotkey(),
                    t.getDisplayColor(),
                    t.getDisplayIcon(),
                    t.getParentId()
            ));
        }
        return result;
    }

    @Override
    @Transactional
    @RequiresPermission(PosQuickItemPermissions.POS_QUICK_ITEM_UPDATE)
    public void batchUpdateLayout(Long tenantId, Long companyId, BatchLayoutRequest request) {
        // Phase 1: verify all IDs belong to tenant/company.
        List<PosQuickItem> tiles = new java.util.ArrayList<>();
        for (BatchLayoutRequest.SlotPlacement slot : request.slots()) {
            PosQuickItem tile = quickItemRepository.findByTenantIdAndCompanyIdAndId(
                    tenantId, companyId, slot.id())
                    .orElseThrow(() -> new PosQuickItemNotFoundException(
                            "Quick item not found with ID: " + slot.id()));
            // Boundary validation
            if (slot.gridRow() < 0 || slot.gridRow() > 2) {
                throw new IllegalArgumentException("gridRow out of bounds: " + slot.gridRow());
            }
            if (slot.gridCol() < 0 || slot.gridCol() > 5) {
                throw new IllegalArgumentException("gridCol out of bounds: " + slot.gridCol());
            }
            tiles.add(tile);
        }

        // Phase 2: shift to temporary negative coordinates to avoid unique collisions.
        for (PosQuickItem tile : tiles) {
            tile.reposition(-1 - tile.getGridRow(), tile.getGridCol(), tile.getSortOrder());
        }
        quickItemRepository.saveAllAndFlush(tiles);

        // Phase 3: apply final positions.
        for (int i = 0; i < tiles.size(); i++) {
            PosQuickItem tile = tiles.get(i);
            BatchLayoutRequest.SlotPlacement slot = request.slots().get(i);
            tile.reposition(slot.gridRow(), slot.gridCol(),
                    slot.gridRow() * 6 + slot.gridCol());
            // update tab/page if needed
            tile.assignTab(request.tabName(), request.pageNumber());
        }
        quickItemRepository.saveAll(tiles);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private PosQuickItem load(Long tenantId, Long companyId, Long id) {
        return quickItemRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new PosQuickItemNotFoundException(
                        "Quick item not found with ID: " + id));
    }

    /**
     * Resolves the catalogue item a tile points at. The snapshot fields are
     * taken from the response, so the tile always shows the item's current
     * code, name and unit.
     */
    private ItemService.ItemResponse resolveItem(Long itemId) {
        return itemService.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item not found with ID: " + itemId));
    }
}
