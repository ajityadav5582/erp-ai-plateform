package com.erp.business.procurement.workflow;

import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.procurement.workflow.dto.WorkflowConfigurationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The company's workflow switches.
 *
 * <p>Tenant and company are read from the signed token rather than taken from the
 * request, exactly as the requisition controller does. A settings endpoint that
 * accepted a company id in its path or body would let a caller who administers one
 * company reconfigure another simply by editing the request - so the scope here is
 * whatever the token says the caller is, and no other.
 *
 * <p><strong>Permissions live on the service, not here.</strong> Reading needs
 * {@code PR_VIEW} and writing needs {@code PR_ADMIN}; both are declared on the
 * methods in {@link WorkflowConfigurationServiceImpl}, which the shared
 * {@code PermissionAspect} enforces. Annotating the controller as well would only
 * give the same two rules a second, divergent place to live.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/procurement/workflow-configuration")
@RequiredArgsConstructor
public class WorkflowConfigurationController {

    private final WorkflowConfigurationService workflowConfigurationService;
    private final BusinessContextAccessor context;

    /**
     * Reads the company's current settings.
     *
     * <p>Always answers, even for a company that has never been configured: the
     * service substitutes the safe default rather than reporting the row missing.
     * A settings screen that had to distinguish "no configuration" from "approval
     * required" would have to guess at one of them, and would guess wrong on the
     * one that matters.
     */
    @GetMapping
    public ResponseEntity<WorkflowConfigurationResponse> get() {
        return ResponseEntity.ok(workflowConfigurationService.get(
                context.getTenantId(), context.getCompanyId()));
    }

    /**
     * Replaces the company's settings.
     *
     * <p>PUT, not PATCH, for the reason given on the requisition update endpoint:
     * the client edits a whole form, so one replacement with one meaning is the
     * honest contract. A partial update would have to define what an absent field
     * means, and every reasonable answer is wrong for a setting that silently
     * changes how spend is authorised.
     */
    @PutMapping
    public ResponseEntity<WorkflowConfigurationResponse> update(
            @Valid @RequestBody WorkflowConfigurationResponse request) {

        return ResponseEntity.ok(workflowConfigurationService.update(
                context.getTenantId(), context.getCompanyId(), request));
    }
}
