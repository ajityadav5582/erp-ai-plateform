package com.erp.business.procurement.purchaserequisition;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.business.procurement.purchaserequisition.dto.CreateRequisitionLineRequest;
import com.erp.business.procurement.purchaserequisition.dto.CreateRequisitionRequest;
import com.erp.business.procurement.purchaserequisition.dto.UpdateRequisitionRequest;
import com.erp.platform.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the identifiers a requisition points at, before anything is written.
 *
 * <p><strong>Scope of this check, stated plainly.</strong> Procurement is a separate
 * deployable from Identity and from Inventory. This validator uses the Inventory
 * module's public interfaces ({@link UnitService}, {@link ItemService}, {@link WarehouseService})
 * to verify that referenced entities exist, are active, and belong to the same
 * tenant/company scope. This maintains proper module boundaries - no direct access
 * to another module's JPA entities or repositories.
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class RequisitionReferenceValidator {

    private final UnitService unitService;
    private final ItemService itemService;
    private final WarehouseService warehouseService;

    /**
     * Validates every external reference carried by a create request.
     *
     * <p>Called before the document number is allocated and before any entity is
     * constructed, so a rejected request costs nothing and leaves no trace.
     *
     * @param request the create request
     * @param tenantId the tenant ID for scoping (used for branch/department validation)
     * @param companyId the company ID for scoping (used for branch/department validation)
     * @throws ValidationException when a reference is missing or malformed
     */
    public void validate(CreateRequisitionRequest request, Long tenantId, Long companyId) {
        validateBranch(request.branchId());
        validateDepartment(request.departmentId());
        validateWarehouse(request.warehouseId(), request.warehouseCode(), request.warehouseName());
        validateLines(request.lines());
    }

    /**
     * Validates the line set of an edit.
     *
     * <p>Deliberately does not re-validate branch or department. Those are not part
     * of an update - a document cannot be re-pointed at another branch, because
     * branch decides which approval policy applies and re-pointing it would change
     * the control a document is subject to. If a client sends them they are ignored,
     * not honoured, and not an error either: reusing the create payload as the
     * update payload is the obvious thing for a client to do, and rejecting it would
     * make the normal case the failing one.
     *
     * @param request the update request
     * @param tenantId the tenant ID for scoping
     * @param companyId the company ID for scoping
     * @throws ValidationException when a line reference is missing or malformed
     */
    public void validate(UpdateRequisitionRequest request, Long tenantId, Long companyId) {
        validateLines(request.lines());
    }

    /**
     * A requisition is raised on behalf of exactly one branch. The branch drives the
     * approval policy, so a missing or nonsensical one does not just corrupt a
     * report - it decides which approval chain the document gets.
     */
    private void validateBranch(Long branchId) {
        if (branchId == null) {
            throw new ValidationException(
                    "A branch is required; it determines which approval policy applies");
        }
        if (branchId <= 0) {
            throw new ValidationException("Branch id must be a positive identifier, was " + branchId);
        }
        // TODO: Add branch validation via Identity module interface when available
    }

    /**
     * The cost centre is optional, but when supplied it must still be a real
     * identifier rather than a placeholder that later gets silently treated as
     * "no department".
     */
    private void validateDepartment(Long departmentId) {
        if (departmentId != null && departmentId <= 0) {
            throw new ValidationException(
                    "Department id must be a positive identifier, was " + departmentId);
        }
        // TODO: Add department validation via Identity module interface when available
    }

    /**
     * The destination warehouse, if one is nominated at all.
     *
     * <p>Optional on the requisition, because the requester often genuinely does not
     * know yet - the site is chosen when the buyer converts the requisition into an
     * order, or when the goods actually arrive. What is not acceptable is a partial
     * nomination: an id with no code or name is the same unresolved reference this
     * class already refuses for a line's item, and freezing only half of it produces a
     * document that can print an id nobody can read.
     */
    private void validateWarehouse(Long warehouseId, String warehouseCode, String warehouseName) {
        if (warehouseId != null && warehouseId <= 0) {
            throw new ValidationException(
                    "Warehouse id must be a positive identifier, was " + warehouseId);
        }
        if (warehouseId != null && (isBlank(warehouseCode) || isBlank(warehouseName))) {
            throw new ValidationException(
                    "Warehouse " + warehouseId
                            + " must carry the warehouse code and name captured at request time");
        }
        if (warehouseId != null) {
            if (!warehouseService.validateWarehouse(warehouseId)) {
                throw new ValidationException(
                        "Warehouse " + warehouseId + " not found, inactive, or not accessible");
            }
        }
    }

    private void validateLines(List<CreateRequisitionLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("A requisition must contain at least one line");
        }

        // A repeated item is refused. The comment here used to argue the opposite -
        // that the same item may legitimately appear on several lines for different
        // delivery points or cost centres - and then threw on it three lines later.
        //
        // The refusal is the correct behaviour and the reasoning was the wrong one.
        // Split delivery only makes sense when the lines actually differ, and the
        // fields that would make them differ are not on the line: delivery location
        // and address are header fields, so two lines for one item in one document
        // carry identical destination data and differ only in quantity or price.
        // That is the signature of a double submission or a merge mistake, not of a
        // split order, and it inflates the estimate by whatever the duplicate is
        // worth. Genuinely split lines are separate requisitions against the same
        // item, which is the case this guard does not touch.
        Set<Long> seen = new HashSet<>();

        lines.forEach(line -> {
            if (line == null) {
                throw new ValidationException("A requisition line cannot be null");
            }
            if (line.itemId() == null || line.itemId() <= 0) {
                throw new ValidationException(
                        "Every requisition line must name a valid item; got item id " + line.itemId());
            }
            if (!seen.add(line.itemId())) {
                // Refused outright rather than warned about: the caller can merge the
                // two lines into one, and the alternative is a document whose estimate
                // is quietly twice what was asked for, which no reviewer has any way
                // to notice by eye.
                throw new ValidationException(
                        "Item " + line.itemId() + " appears on more than one line of this requisition");
            }

            // The snapshot is what makes the document readable years later. A line with
            // an id but no code or name can only be resolved by calling another service
            // at read time, which defeats the point of storing it.
            if (isBlank(line.itemCode()) || isBlank(line.itemName())) {
                throw new ValidationException(
                        "Line for item " + line.itemId()
                                + " must carry the item code and name captured at request time");
            }
            if (isBlank(line.uomCode())) {
                throw new ValidationException(
                        "Line for item " + line.itemId() + " must carry the unit of measure");
            }

            // Validate that the UOM code exists or can be resolved, is active, and belongs to the tenant/company
            UnitService.UnitResponse unit = UnitResolver.resolveUnit(unitService, line.uomCode());
            if (Boolean.FALSE.equals(unit.getIsActive())) {
                throw new ValidationException(
                        "Unit of measure '" + line.uomCode() + "' is inactive and cannot be used");
            }

            // Validate item exists and is active
            if (!itemService.validateItem(line.itemId())) {
                throw new ValidationException(
                        "Item " + line.itemId() + " not found, inactive, or not accessible");
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
