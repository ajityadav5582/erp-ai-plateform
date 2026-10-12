package com.erp.business.procurement.workflow;

import com.erp.business.procurement.shared.security.PurchaseRequisitionPermissions;
import com.erp.business.procurement.workflow.domain.CompanyWorkflowConfiguration;
import com.erp.business.procurement.workflow.dto.WorkflowConfigurationResponse;
import com.erp.business.procurement.workflow.infrastructure.persistence.CompanyWorkflowConfigurationRepository;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes the company's workflow switches.
 *
 * <p><strong>Authorisation.</strong> Reading is gated on {@code PR_VIEW} because
 * every requisition screen needs to know whether the approval queue is in play to
 * render the right affordances. Writing is gated on {@code PR_ADMIN}, not on
 * {@code PR_CREATE} or {@code PR_APPROVE}: turning the approval requirement off is
 * a governance decision about the company, not an act of procurement, and it
 * belongs to whoever administers the module rather than to whoever happens to
 * raise requisitions. A holder of {@code PR_APPROVE} can sign off spend but
 * cannot switch the control off.
 *
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowConfigurationServiceImpl implements WorkflowConfigurationService {

    /**
     * What an unconfigured company gets.
     *
     * <p>Approval required. A company that has never been configured has not
     * decided anything about its spend control, and the only thing that can be
     * inferred from silence is that it should behave as it did before the switch
     * existed. Defaulting to {@code false} would switch off an internal control
     * for every existing company the day this row's absence is first observed.
     */
    private static final boolean DEFAULT_APPROVAL_REQUIRED = true;

    private final CompanyWorkflowConfigurationRepository configurationRepository;

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(PurchaseRequisitionPermissions.VIEW)
    public WorkflowConfigurationResponse get(Long tenantId, Long companyId) {
        requireScope(tenantId, companyId);
        return toResponse(isPurchaseRequisitionApprovalRequired(tenantId, companyId));
    }

    @Override
    @Transactional
    @RequiresPermission(PurchaseRequisitionPermissions.ADMIN)
    public WorkflowConfigurationResponse update(Long tenantId, Long companyId,
                                               WorkflowConfigurationResponse request) {
        requireScope(tenantId, companyId);
        if (request == null || request.purchaseRequisitionApprovalRequired() == null) {
            // A null here is not "leave it as it is" - this is a whole-document
            // PUT. Silently treating an absent field as the default would let a
            // client that forgot the property switch a company's approval
            // requirement off without ever asking for it.
            throw new ValidationException(
                    "purchaseRequisitionApprovalRequired is required; this endpoint replaces "
                            + "the whole configuration rather than patching it");
        }

        boolean required = request.purchaseRequisitionApprovalRequired();
        CompanyWorkflowConfiguration configuration = configurationRepository
                .findByTenantIdAndCompanyId(tenantId, companyId)
                .map(existing -> {
                    existing.requirePurchaseRequisitionApproval(required);
                    return existing;
                })
                .orElseGet(() -> CompanyWorkflowConfiguration.create(tenantId, companyId, required));

        configurationRepository.save(configuration);

        if (!required) {
            // Worth a line in the log: this is the one setting whose absence of
            // effect is invisible afterwards. Nothing about a requisition looks
            // different once approved, so an auditor reading the trail a year
            // later needs the log to know the control was company configuration
            // rather than an approver's decision.
            log.warn("Company {} has turned OFF purchase-requisition approval. Requisitions "
                            + "will now be approved automatically at submission. Only requisitions "
                            + "submitted from now on are affected; anything already in an approval "
                            + "queue keeps the chain it was submitted with.",
                    companyId);
        }

        return toResponse(required);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPurchaseRequisitionApprovalRequired(Long tenantId, Long companyId) {
        if (tenantId == null || companyId == null) {
            return DEFAULT_APPROVAL_REQUIRED;
        }
        return configurationRepository.findByTenantIdAndCompanyId(tenantId, companyId)
                .map(CompanyWorkflowConfiguration::isPurchaseRequisitionApprovalRequired)
                .orElse(DEFAULT_APPROVAL_REQUIRED);
    }

    private static WorkflowConfigurationResponse toResponse(boolean approvalRequired) {
        return new WorkflowConfigurationResponse(approvalRequired);
    }

    private static void requireScope(Long tenantId, Long companyId) {
        if (tenantId == null || companyId == null) {
            throw new ValidationException("Tenant and company context are required");
        }
    }
}
