package com.erp.business.pos.menu;

import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.pos.shared.security.PosQuickItemPermissions;
import com.erp.business.pos.shared.security.PosRegisterPermissions;
import com.erp.business.pos.shared.security.PosScanPermissions;
import com.erp.business.pos.shared.security.PosSessionPermissions;
import com.erp.business.pos.shared.security.PosTransactionPermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * POS menu configuration endpoint.
 *
 * <p>Returns the navigation menu structure for the POS module based on the
 * authenticated user's permissions. The frontend uses this to dynamically render
 * the POS submenu instead of hardcoding it - the same contract the Sales and
 * Procurement modules expose, so the sidebar treats all three identically.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/pos/menu")
@RequiredArgsConstructor
public class PosMenuController {

    private final BusinessContextAccessor context;

    /**
     * Returns the POS menu structure for the current user. Each item carries the
     * permission that gates it; the frontend renders only what the user may see.
     */
    @GetMapping
    @RequiresPermission(PosScanPermissions.POS_SCAN)
    public ResponseEntity<PosMenuResponse> getMenu() {
        List<MenuItem> items = new ArrayList<>();

        items.add(new MenuItem(
                "Terminal", "/pos/terminal", "scan",
                PosScanPermissions.POS_SCAN,
                "Ring up sales: scan items, take payment, print receipts"));

        items.add(new MenuItem(
                "Transactions", "/pos/transactions", "receipt",
                PosTransactionPermissions.POS_TRANSACTION_VIEW,
                "View and void completed sales"));

        items.add(new MenuItem(
                "Sessions", "/pos/sessions", "clock",
                PosSessionPermissions.POS_SESSION_VIEW,
                "Open and close cashier shifts"));

        items.add(new MenuItem(
                "Registers", "/pos/registers", "monitor",
                PosRegisterPermissions.POS_REGISTER_VIEW,
                "Manage POS terminals and their warehouses"));

        items.add(new MenuItem(
                "Quick Items", "/pos/quick-items", "grid",
                PosQuickItemPermissions.POS_QUICK_ITEM_VIEW,
                "Configure the terminal's quick-access item grid"));

        return ResponseEntity.ok(new PosMenuResponse(items));
    }

    /** Response containing the POS menu items. */
    public record PosMenuResponse(List<MenuItem> items) {}

    /**
     * A single menu item in the POS navigation.
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
