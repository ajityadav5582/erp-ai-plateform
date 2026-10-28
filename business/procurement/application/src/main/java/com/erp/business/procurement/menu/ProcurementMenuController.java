package com.erp.business.procurement.menu;

import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.procurement.shared.security.ProcurementPermissions;
import com.erp.business.procurement.shared.security.PurchaseOrderPermissions;
import com.erp.business.procurement.shared.security.PurchaseRequisitionPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Procurement menu configuration endpoint.
 *
 * <p>Returns the navigation menu structure for the Procurement module based on the
 * authenticated user's permissions. The frontend uses this to dynamically render
 * the procurement submenu instead of hardcoding it.
 *
 * <p>Permissions are checked via the {@code @RequiresPermission} annotation on each
 * menu item's required permission. The service filters out items the user cannot access.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/procurement/menu")
@RequiredArgsConstructor
public class ProcurementMenuController {

    private final BusinessContextAccessor context;

    /**
     * Returns the procurement menu structure for the current user.
     *
     * <p>Each menu item includes the required permission code. The frontend should
     * only render items for which the user has the corresponding permission (the
     * backend already filters based on permissions, but the frontend can also use
     * this for additional UI logic).
     */
    @GetMapping
    @RequiresPermission(PurchaseRequisitionPermissions.VIEW) // Base permission to access procurement menu
    public ResponseEntity<ProcurementMenuResponse> getMenu() {
        List<MenuItem> items = new ArrayList<>();

        // Purchase Requisitions
        items.add(new MenuItem(
                "Purchase Requisitions",
                "/purchase-requisitions",
                "file-text",
                PurchaseRequisitionPermissions.VIEW,
                "View and manage purchase requisitions"
        ));

        // Purchase Orders
        items.add(new MenuItem(
                "Purchase Orders",
                "/purchase-orders",
                "shopping-cart",
                PurchaseOrderPermissions.VIEW,
                "View and manage purchase orders"
        ));

        // Goods Receipts
        items.add(new MenuItem(
                "Goods Receipts",
                "/goods-receipts",
                "truck",
                ProcurementPermissions.GOODS_RECEIPT_VIEW,
                "Record and view goods receipts"
        ));

        // Purchase Invoices
        items.add(new MenuItem(
                "Purchase Invoices",
                "/purchase-invoices",
                "file-invoice",
                ProcurementPermissions.PURCHASE_INVOICE_VIEW,
                "View and manage purchase invoices"
        ));

        // Supplier Payments
        items.add(new MenuItem(
                "Supplier Payments",
                "/supplier-payments",
                "credit-card",
                ProcurementPermissions.SUPPLIER_PAYMENT_VIEW,
                "View and manage supplier payments"
        ));

        // Workflow Configuration (admin only)
        items.add(new MenuItem(
                "Workflow Configuration",
                "/workflow-configuration",
                "settings",
                PurchaseRequisitionPermissions.ADMIN,
                "Configure procurement workflow settings"
        ));

        return ResponseEntity.ok(new ProcurementMenuResponse(items));
    }

    /**
     * Response containing the procurement menu items.
     */
    public record ProcurementMenuResponse(List<MenuItem> items) {}

    /**
     * A single menu item in the procurement navigation.
     *
     * @param title       Display title
     * @param href        Frontend route path
     * @param icon        Icon name (frontend maps this to an icon component)
     * @param permission  Required permission code
     * @param description Tooltip/description
     */
    public record MenuItem(
            String title,
            String href,
            String icon,
            String permission,
            String description
    ) {}
}
