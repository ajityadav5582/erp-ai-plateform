package com.erp.business.procurement.purchaserequisition;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import com.erp.business.procurement.purchaserequisition.dto.ApprovalDecisionRequest;
import com.erp.business.procurement.purchaserequisition.dto.CancelRequisitionRequest;
import com.erp.business.procurement.purchaserequisition.dto.CreateRequisitionRequest;
import com.erp.business.procurement.purchaserequisition.dto.OrderAwardRequest;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionActionabilityResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionDetailResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionListResponse;
import com.erp.business.procurement.purchaserequisition.dto.UpdateRequisitionRequest;
import com.erp.business.procurement.shared.context.BusinessContextAccessor;
import com.erp.platform.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/v1/procurement/purchase-requisitions")
@RequiredArgsConstructor
public class PurchaseRequisitionController {

    private static final String BASE_PATH = "/api/v1/procurement/purchase-requisitions";

    private final PurchaseRequisitionService requisitionService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<RequisitionDetailResponse> create(
            @Valid @RequestBody CreateRequisitionRequest request,
            UriComponentsBuilder uriBuilder) {

        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        RequisitionDetailResponse response =
                requisitionService.create(tenantId, companyId, currentUserId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Replaces the content of a draft or returned requisition.
     *
     * <p>PUT rather than PATCH because the client edits a whole grid: one
     * replacement has exactly one meaning, whereas a partial update would have to
     * define what "absent" means for every field and would get it wrong in a way
     * that silently deletes data.
     */
    @PutMapping("/{requisitionId}")
    public ResponseEntity<RequisitionDetailResponse> update(
            @PathVariable Long requisitionId,
            @Valid @RequestBody UpdateRequisitionRequest request) {

        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.update(tenantId, companyId, requisitionId, request, currentUser()));
    }

    @GetMapping
    public ResponseEntity<Page<RequisitionListResponse>> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) PurchaseRequisitionStatus status,
            Pageable pageable) {

        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.list(tenantId, companyId, branchId, status, pageable));
    }

    /**
     * Requisitions the caller raised.
     *
     * <p>Declared before {@code /{requisitionId}} only for readability; Spring
     * matches the literal segment ahead of the path variable regardless of order.
     */
    @GetMapping("/mine")
    public ResponseEntity<Page<RequisitionListResponse>> listMine(Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.listMine(tenantId, companyId, currentUserId(), pageable));
    }

    /**
     * Requisitions whose current approval step is the caller's.
     */
    @GetMapping("/approval-inbox")
    public ResponseEntity<Page<RequisitionListResponse>> approvalInbox(Pageable pageable) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.approvalInbox(tenantId, companyId, currentUserId(), pageable));
    }

    @GetMapping("/{requisitionId}")
    public ResponseEntity<RequisitionDetailResponse> getById(@PathVariable Long requisitionId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(requisitionService.getById(tenantId, companyId, requisitionId));
    }

    /**
     * What the caller may do with this requisition right now.
     *
     * <p>Exists so the UI stops rendering buttons that 403 when pressed. Advisory
     * only: every action is re-authorised on the endpoint that performs it.
     */
    @GetMapping("/{requisitionId}/actions")
    public ResponseEntity<RequisitionActionabilityResponse> actions(
            @PathVariable Long requisitionId) {

        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.actionability(tenantId, companyId, requisitionId, currentUser()));
    }

    @PostMapping("/{requisitionId}/submit")
    public ResponseEntity<RequisitionDetailResponse> submit(@PathVariable Long requisitionId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.submit(tenantId, companyId, requisitionId, currentUser()));
    }

    @PostMapping("/{requisitionId}/decisions")
    public ResponseEntity<RequisitionDetailResponse> decide(
            @PathVariable Long requisitionId,
            @Valid @RequestBody ApprovalDecisionRequest request) {

        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.decide(tenantId, companyId, requisitionId, request, currentUser()));
    }

    @PostMapping("/{requisitionId}/order-awards")
    public ResponseEntity<RequisitionDetailResponse> awardToOrder(
            @PathVariable Long requisitionId,
            @Valid @RequestBody OrderAwardRequest request) {

        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.awardToOrder(tenantId, companyId, requisitionId, request, currentUser()));
    }

    @PostMapping("/{requisitionId}/cancel")
    public ResponseEntity<RequisitionDetailResponse> cancel(
            @PathVariable Long requisitionId,
            @Valid @RequestBody CancelRequisitionRequest request) {

        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        return ResponseEntity.ok(
                requisitionService.cancel(tenantId, companyId, requisitionId, request.reason(), currentUser()));
    }

    @DeleteMapping("/{requisitionId}")
    public ResponseEntity<Void> delete(@PathVariable Long requisitionId) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();
        requisitionService.delete(tenantId, companyId, requisitionId, currentUser());
        return ResponseEntity.noContent().build();
    }

    private String currentUser() {
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("An authenticated user is required for this operation");
        }
        return userId;
    }

    private Long currentUserId() {
        try {
            return Long.valueOf(currentUser());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("The authenticated user id is not a numeric identity id");
        }
    }
}
