package com.erp.business.pos.quickitem;

import com.erp.business.pos.quickitem.dto.CreateQuickItemRequest;
import com.erp.business.pos.quickitem.dto.PosQuickItemResponse;
import com.erp.business.pos.quickitem.dto.ReorderQuickItemsRequest;
import com.erp.business.pos.quickitem.dto.UpdateQuickItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * POS quick-item (terminal grid tile) use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from
 * the payload. Authorisation is declared with {@code @RequiresPermission} on
 * the implementation and enforced by the shared aspect before the body runs.
 */
public interface PosQuickItemService {

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    PosQuickItemResponse createQuickItem(Long tenantId, Long companyId, CreateQuickItemRequest request);

    PosQuickItemResponse updateQuickItem(Long tenantId, Long companyId, Long id,
                                         UpdateQuickItemRequest request);

    PosQuickItemResponse activateQuickItem(Long tenantId, Long companyId, Long id);

    PosQuickItemResponse deactivateQuickItem(Long tenantId, Long companyId, Long id);

    /**
     * Removes a tile from the grid. Quick items are pure terminal configuration
     * with no transactional history, so removal is a hard delete.
     */
    void deleteQuickItem(Long tenantId, Long companyId, Long id);

    /**
     * Persists a whole dragged grid layout in one atomic batch. Tiles absent
     * from the request keep their current placement.
     */
    List<PosQuickItemResponse> reorderQuickItems(Long tenantId, Long companyId,
                                                 ReorderQuickItemsRequest request);

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    PosQuickItemResponse getQuickItemById(Long tenantId, Long companyId, Long id);

    /**
     * The terminal grid load: every company-shared tile plus the tiles pinned
     * to one register, active first, ordered by grid position.
     */
    List<PosQuickItemResponse> loadGrid(Long tenantId, Long companyId, Long registerId);

    Page<PosQuickItemResponse> listQuickItems(Long tenantId, Long companyId, Pageable pageable);

    Page<PosQuickItemResponse> listQuickItemsByRegister(Long tenantId, Long companyId,
                                                        Long registerId, Pageable pageable);

    Page<PosQuickItemResponse> listCompanySharedQuickItems(Long tenantId, Long companyId,
                                                           Pageable pageable);

    Page<PosQuickItemResponse> searchQuickItems(Long tenantId, Long companyId, String term,
                                                Pageable pageable);
}
