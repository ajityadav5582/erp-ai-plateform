package com.erp.business.procurement.purchaserequisition;

import com.erp.business.inventory.interfaces.unit.UnitService;
import com.erp.platform.common.exception.ValidationException;

import java.util.Optional;

/**
 * Utility for resolving units of measure flexibly by code, alias, name, or fallback.
 *
 * <p>This class now delegates to the {@link UnitService} interface from the
 * {@code business:inventory:interfaces} module, maintaining proper module boundaries.
 * The actual resolution logic lives in the Inventory module's adapter implementation.
 */
public final class UnitResolver {

    private UnitResolver() {}

    /**
     * Resolves a Unit from the given UOM string using the Inventory module's UnitService.
     *
     * @param unitService the unit service interface
     * @param rawUomCode the input UOM string (e.g. "PIECES", "PCS", "KG")
     * @return the resolved Unit DTO
     */
    public static UnitService.UnitResponse resolveUnit(UnitService unitService, String rawUomCode) {
        if (rawUomCode == null || rawUomCode.isBlank()) {
            // Fallback to first active unit
            return unitService.findAllActive().stream().findFirst()
                    .orElseThrow(() -> new ValidationException("No valid unit of measure found"));
        }

        String code = rawUomCode.trim().toUpperCase();

        // 1. Direct match by code
        Optional<UnitService.UnitResponse> match = unitService.findByCode(code);
        if (match.isPresent()) {
            return match.get();
        }

        // 2. Known UOM code alias mapping
        String aliasCode = mapUomAlias(code);
        if (aliasCode != null && !aliasCode.equalsIgnoreCase(code)) {
            match = unitService.findByCode(aliasCode);
            if (match.isPresent()) {
                return match.get();
            }
        }

        // 3. Search by name (using the resolveUnit method which does flexible matching)
        match = unitService.resolveUnit(rawUomCode);
        if (match.isPresent()) {
            return match.get();
        }

        // 4. Fallback to any active unit
        return unitService.findAllActive().stream().findFirst()
                .orElseThrow(() -> new ValidationException("No valid unit of measure found"));
    }

    private static String mapUomAlias(String code) {
        return switch (code) {
            case "PIECES", "PIECE", "PC", "EA", "EACH", "NOS", "NO", "NUMBERS", "NUMBER" -> "PCS";
            case "KGS", "KILOGRAM", "KILOGRAMS", "KILO" -> "KG";
            case "LTR", "LTRS", "LITRE", "LITRES", "LITER", "LITERS" -> "L";
            case "BOXES", "PACKET", "PACKETS", "PKT", "PKTS", "BOX" -> "BOX";
            case "METERS", "METER", "MTR", "MTRS" -> "M";
            case "HOURS", "HOUR", "HRS", "HR" -> "HR";
            default -> null;
        };
    }
}
