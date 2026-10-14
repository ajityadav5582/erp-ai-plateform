package com.erp.business.sales.menu;

import com.erp.business.sales.shared.context.BusinessContextAccessor;
import com.erp.business.sales.shared.security.CustomerPermissions;
import com.erp.business.sales.shared.security.CustomerReceiptPermissions;
import com.erp.business.sales.shared.security.SalesDeliveryPermissions;
import com.erp.business.sales.shared.security.SalesInvoicePermissions;
import com.erp.business.sales.shared.security.SalesOrderPermissions;
import com.erp.business.sales.shared.security.SalesReturnPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Sales menu configuration endpoint.
 *
 * <p>Returns the navigation menu structure for the Sales module based on the
 * authenticated user's permissions. The frontend uses this to dynamically render
 * the sales submenu instead of hardcoding it - the same contract the Procurement
 * module exposes, so the sidebar treats both identically.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sales/menu")
@RequiredArgsConstructor
public class SalesMenuController {

    private final BusinessContextAccessor context;

    /**
     * Returns the sales menu structure for the current user. Each item carries the
     * permission that gates it; the frontend renders only what the user may see.
     */
    @GetMapping
    @RequiresPermission(CustomerPermissions.CUSTOMER_VIEW)
    public ResponseEntity<SalesMenuResponse> getMenu() {
        List<MenuItem> items = new ArrayList<>();

        items.add(new MenuItem(
                "Customers", "/customers", "users",
                CustomerPermissions.CUSTOMER_VIEW,
                "View and manage customers"));

        items.add(new MenuItem(
                "Sales Quotations", "/sales-quotations", "file-text",
                SalesOrderPermissions.SALES_ORDER_VIEW,
                "Price quotes and estimates for clients"));

        items.add(new MenuItem(
                "Sales Orders", "/sales-orders", "shopping-cart",
                SalesOrderPermissions.SALES_ORDER_VIEW,
                "View and manage sales orders"));

        items.add(new MenuItem(
                "Deliveries", "/deliveries", "truck",
                SalesDeliveryPermissions.SALES_DELIVERY_VIEW,
                "Record and view goods deliveries"));

        items.add(new MenuItem(
                "Sales Invoices", "/sales-invoices", "file-invoice",
                SalesInvoicePermissions.SALES_INVOICE_VIEW,
                "View and manage IRD tax invoices"));

        items.add(new MenuItem(
                "Customer Receipts", "/customer-receipts", "credit-card",
                CustomerReceiptPermissions.CUSTOMER_RECEIPT_VIEW,
                "View and manage customer receipts"));

        items.add(new MenuItem(
                "Sales Returns", "/sales-returns", "undo",
                SalesReturnPermissions.SALES_RETURN_VIEW,
                "View and manage credit notes"));

        items.add(new MenuItem(
                "Sales Register", "/sales-register", "bar-chart",
                SalesInvoicePermissions.SALES_INVOICE_VIEW,
                "VAT-201 sales register for a period"));

        return ResponseEntity.ok(new SalesMenuResponse(items));
    }

    /** Response containing the sales menu items. */
    public record SalesMenuResponse(List<MenuItem> items) {}

    /**
     * A single menu item in the sales navigation.
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
