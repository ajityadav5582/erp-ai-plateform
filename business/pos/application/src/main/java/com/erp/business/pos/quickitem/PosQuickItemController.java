package com.erp.business.pos.quickitem;

import com.erp.business.pos.quickitem.dto.CreateQuickItemRequest;
import com.erp.business.pos.quickitem.dto.PosQuickItemResponse;
import com.erp.business.pos.quickitem.dto.ReorderQuickItemsRequest;
import com.erp.business.pos.quickitem.dto.UpdateQuickItemRequest;
import com.erp.business.pos.shared.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * POS quick-item endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, enforced by the shared aspect. The controller only
 * reads the authenticated tenant and company, so a caller cannot widen its own
 * scope through the payload.
 *
 * <p>The terminal's hot path is {@code GET /grid?registerId=} — one call returns
 * every tile the register should render.
 */
@RestController
@RequestMapping("/api/v1/pos/quick-items")
@RequiredArgsConstructor
public class PosQuickItemController {

    private static final String BASE_PATH = "/api/v1/pos/quick-items";

    private final PosQuickItemService quickItemService;
    private final BusinessContextAccessor context;

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<PosQuickItemResponse> createQuickItem(
            @Valid @RequestBody CreateQuickItemRequest request,
            UriComponentsBuilder uriBuilder) {

        PosQuickItemResponse response = quickItemService.createQuickItem(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PosQuickItemResponse> updateQuickItem(
            @PathVariable Long id, @Valid @RequestBody UpdateQuickItemRequest request) {
        return ResponseEntity.ok(quickItemService.updateQuickItem(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<PosQuickItemResponse> activateQuickItem(@PathVariable Long id) {
        return ResponseEntity.ok(quickItemService.activateQuickItem(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<PosQuickItemResponse> deactivateQuickItem(@PathVariable Long id) {
        return ResponseEntity.ok(quickItemService.deactivateQuickItem(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuickItem(@PathVariable Long id) {
        quickItemService.deleteQuickItem(context.getTenantId(), context.getCompanyId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<PosQuickItemResponse>> reorderQuickItems(
            @Valid @RequestBody ReorderQuickItemsRequest request) {
        return ResponseEntity.ok(quickItemService.reorderQuickItems(
                context.getTenantId(), context.getCompanyId(), request));
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    @GetMapping("/grid")
    public ResponseEntity<List<PosQuickItemResponse>> loadGrid(@RequestParam("registerId") Long registerId) {
        return ResponseEntity.ok(quickItemService.loadGrid(
                context.getTenantId(), context.getCompanyId(), registerId));
    }

    @GetMapping
    public ResponseEntity<Page<PosQuickItemResponse>> listQuickItems(
            @PageableDefault(size = 50, sort = {"sortOrder", "gridRow", "gridCol", "id"},
                    direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(quickItemService.listQuickItems(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PosQuickItemResponse>> searchQuickItems(
            @RequestParam("term") String term,
            @PageableDefault(size = 50, sort = {"sortOrder", "gridRow", "gridCol", "id"},
                    direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(quickItemService.searchQuickItems(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-register")
    public ResponseEntity<Page<PosQuickItemResponse>> listQuickItemsByRegister(
            @RequestParam("registerId") Long registerId,
            @PageableDefault(size = 50, sort = {"sortOrder", "gridRow", "gridCol", "id"},
                    direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(quickItemService.listQuickItemsByRegister(
                context.getTenantId(), context.getCompanyId(), registerId, pageable));
    }

    @GetMapping("/company-shared")
    public ResponseEntity<Page<PosQuickItemResponse>> listCompanySharedQuickItems(
            @PageableDefault(size = 50, sort = {"sortOrder", "gridRow", "gridCol", "id"},
                    direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(quickItemService.listCompanySharedQuickItems(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosQuickItemResponse> getQuickItemById(@PathVariable Long id) {
        return ResponseEntity.ok(quickItemService.getQuickItemById(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
