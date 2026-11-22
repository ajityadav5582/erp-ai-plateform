package com.erp.business.inventory.items;

import com.erp.business.inventory.domain.Category;
import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.domain.ItemType;
import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.business.inventory.domain.Unit;
import com.erp.business.inventory.domain.Warehouse;
import com.erp.business.inventory.infrastructure.persistence.CategoryRepository;
import com.erp.business.inventory.infrastructure.persistence.ItemRepository;
import com.erp.business.inventory.infrastructure.persistence.UnitRepository;
import com.erp.business.inventory.infrastructure.persistence.WarehouseRepository;
import com.erp.business.inventory.items.dto.CreateItemRequest;
import com.erp.business.inventory.items.dto.ItemImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemCsvServiceImpl implements ItemCsvService {

    private final ItemRepository itemRepository;
    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemService itemService;

    private static final String CSV_HEADER = "SKU,Name (En),Name (Np),UOM Code,Category,Item Type,Taxability Type,VAT Rate,Selling Price,Purchase Price,Is Inventory Tracked,Opening Quantity,Opening Cost,Reorder Level,Min Stock,Barcode,HS Code,Description";

    @Override
    public byte[] generateTemplateCsv() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM so Excel automatically recognizes unicode (Nepali text)
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            writer.println(CSV_HEADER);
            writer.println("TAP-BR-01,\"Brass Water Tap 1/2\\\"\",पानीको धारा १/२\",PCS,Sanitary,GOODS,TAXABLE,13.00,450.00,320.00,true,50,320.00,10,5,890123450001,,Heavy duty brass bib tap");
            writer.println("PVC-110,PVC Pipe 110mm 4-inch,पिभिसी पाइप ४ इन्च,PCS,Plumbing,GOODS,TAXABLE,13.00,850.00,680.00,true,20,680.00,5,2,890123450002,3917.22.00,Heavy duty drain pipe");
            writer.println("SERV-INST,Plumbing Installation Service,प्लम्बिङ जडान सेवा,JOB,Services,SERVICE,TAXABLE,13.00,1500.00,0.00,false,,,,,,,Per-visit installation and testing");
            writer.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV template", e);
        }
    }

    @Override
    public byte[] exportItemsCsv(Long tenantId, Long companyId) {
        List<Item> items = itemRepository.findByTenantIdAndCompanyIdOrderBySkuAsc(tenantId, companyId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            writer.println(CSV_HEADER);
            for (Item item : items) {
                String uomCode = item.getUom() != null ? item.getUom().getCode() : "";
                String categoryName = item.getCategory() != null ? item.getCategory().getName() : "";
                String itemType = item.getItemType() != null ? item.getItemType().name() : "GOODS";
                String taxability = item.getTaxabilityType() != null ? item.getTaxabilityType().name() : "TAXABLE";

                writer.println(String.join(",",
                        escapeCsv(item.getSku()),
                        escapeCsv(item.getNameEn()),
                        escapeCsv(item.getNameNp()),
                        escapeCsv(uomCode),
                        escapeCsv(categoryName),
                        escapeCsv(itemType),
                        escapeCsv(taxability),
                        escapeCsv(item.getVatRate() != null ? item.getVatRate().toPlainString() : "13.00"),
                        escapeCsv(item.getSellingPrice() != null ? item.getSellingPrice().toPlainString() : "0.00"),
                        escapeCsv(item.getPurchasePrice() != null ? item.getPurchasePrice().toPlainString() : "0.00"),
                        escapeCsv(item.getIsInventoryTracked() != null ? item.getIsInventoryTracked().toString() : "true"),
                        "", // Opening Quantity
                        "", // Opening Cost
                        escapeCsv(item.getReorderLevel() != null ? item.getReorderLevel().toPlainString() : ""),
                        escapeCsv(item.getMinStockLevel() != null ? item.getMinStockLevel().toPlainString() : ""),
                        escapeCsv(item.getBarcode()),
                        escapeCsv(item.getHsCode()),
                        escapeCsv(item.getDescriptionEn())
                ));
            }
            writer.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export items to CSV", e);
        }
    }

    @Override
    public ItemImportResult importItemsCsv(Long tenantId, Long companyId, InputStream inputStream) {
        List<ItemImportResult.RowError> errors = new ArrayList<>();
        int totalRows = 0;
        int importedCount = 0;

        // Preload active UOMs for this company
        List<Unit> units = unitRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(tenantId, companyId);
        Map<String, Unit> uomByCodeOrName = new HashMap<>();
        for (Unit u : units) {
            uomByCodeOrName.put(u.getCode().trim().toUpperCase(Locale.ROOT), u);
            uomByCodeOrName.put(u.getName().trim().toLowerCase(Locale.ROOT), u);
        }

        // Preload active Categories
        List<Category> categories = categoryRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(tenantId, companyId);
        Map<String, Category> categoryByName = new HashMap<>();
        for (Category c : categories) {
            categoryByName.put(c.getName().trim().toLowerCase(Locale.ROOT), c);
        }

        // Preload default warehouse for opening stock
        Long defaultWarehouseId = warehouseRepository.findDefaultWarehouse(tenantId, companyId)
                .map(Warehouse::getId)
                .orElseGet(() -> {
                    List<Warehouse> active = warehouseRepository.findByTenantIdAndCompanyIdAndIsActiveTrueOrderByCodeAsc(tenantId, companyId);
                    return active.isEmpty() ? null : active.get(0).getId();
                });

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<List<String>> csvRows = parseCsv(reader);
            if (csvRows.isEmpty()) {
                return new ItemImportResult(0, 0, 0, List.of(new ItemImportResult.RowError(1, "", "", "CSV file is empty")));
            }

            // Map header column indices
            List<String> headerRow = csvRows.get(0);
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headerRow.size(); i++) {
                String normalized = normalizeHeader(headerRow.get(i));
                if (!normalized.isEmpty()) {
                    colIndex.put(normalized, i);
                }
            }

            for (int r = 1; r < csvRows.size(); r++) {
                totalRows++;
                int rowNumber = r + 1; // 1-based row index in file
                List<String> row = csvRows.get(r);
                if (row.isEmpty() || row.stream().allMatch(String::isBlank)) {
                    continue; // Skip blank lines
                }

                String sku = getCol(row, colIndex, "sku");
                String nameEn = getCol(row, colIndex, "nameen", "name", "itemname");
                String nameNp = getCol(row, colIndex, "namenp", "nepaliname");
                String uomStr = getCol(row, colIndex, "uomcode", "uom", "unit");
                String catStr = getCol(row, colIndex, "category", "categoryname");
                String itemTypeStr = getCol(row, colIndex, "itemtype", "type");
                String taxabilityStr = getCol(row, colIndex, "taxabilitytype", "taxability");
                String vatRateStr = getCol(row, colIndex, "vatrate", "vat");
                String sellingPriceStr = getCol(row, colIndex, "sellingprice", "selling", "price");
                String purchasePriceStr = getCol(row, colIndex, "purchaseprice", "costprice", "purchase");
                String trackedStr = getCol(row, colIndex, "isinventorytracked", "inventorytracked", "tracked");
                String openingQtyStr = getCol(row, colIndex, "openingquantity", "openingqty", "openingstock");
                String openingCostStr = getCol(row, colIndex, "openingcost", "openingcostprice", "initialcost");
                String reorderLevelStr = getCol(row, colIndex, "reorderlevel", "reorder");
                String minStockStr = getCol(row, colIndex, "minstock", "minstocklevel");
                String barcode = getCol(row, colIndex, "barcode");
                String hsCode = getCol(row, colIndex, "hscode");
                String description = getCol(row, colIndex, "description", "notes");

                // Validation
                if (StringUtils.isBlank(nameEn)) {
                    errors.add(new ItemImportResult.RowError(rowNumber, sku, "", "Item name (English) is required"));
                    continue;
                }

                if (StringUtils.isBlank(uomStr)) {
                    errors.add(new ItemImportResult.RowError(rowNumber, sku, nameEn, "UOM code or unit is required"));
                    continue;
                }

                Unit resolvedUom = uomByCodeOrName.get(uomStr.trim().toUpperCase(Locale.ROOT));
                if (resolvedUom == null) {
                    resolvedUom = uomByCodeOrName.get(uomStr.trim().toLowerCase(Locale.ROOT));
                }
                if (resolvedUom == null) {
                    errors.add(new ItemImportResult.RowError(rowNumber, sku, nameEn, "Unit of Measure '" + uomStr + "' not found"));
                    continue;
                }

                Long categoryId = null;
                if (StringUtils.isNotBlank(catStr)) {
                    Category cat = categoryByName.get(catStr.trim().toLowerCase(Locale.ROOT));
                    if (cat != null) {
                        categoryId = cat.getId();
                    }
                }

                // If SKU is blank, generate a safe unique code
                if (StringUtils.isBlank(sku)) {
                    sku = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
                }

                ItemType itemType = parseItemType(itemTypeStr);
                TaxabilityType taxabilityType = parseTaxabilityType(taxabilityStr);
                BigDecimal vatRate = parseBigDecimal(vatRateStr, taxabilityType == TaxabilityType.TAXABLE ? new BigDecimal("13.00") : BigDecimal.ZERO);
                BigDecimal sellingPrice = parseBigDecimal(sellingPriceStr, BigDecimal.ZERO);
                BigDecimal purchasePrice = parseBigDecimal(purchasePriceStr, BigDecimal.ZERO);
                boolean isTracked = parseBoolean(trackedStr, itemType != ItemType.SERVICE);

                BigDecimal openingQty = parseBigDecimal(openingQtyStr, null);
                BigDecimal openingCost = parseBigDecimal(openingCostStr, purchasePrice);
                Long openingWhId = (openingQty != null && openingQty.compareTo(BigDecimal.ZERO) > 0) ? defaultWarehouseId : null;

                BigDecimal reorderLevel = parseBigDecimal(reorderLevelStr, null);
                BigDecimal minStock = parseBigDecimal(minStockStr, null);

                CreateItemRequest request = new CreateItemRequest(
                        sku.trim(),
                        StringUtils.trimToNull(barcode),
                        StringUtils.trimToNull(hsCode),
                        nameEn.trim(),
                        StringUtils.trimToNull(nameNp),
                        StringUtils.trimToNull(description),
                        null, // descriptionNp
                        categoryId,
                        resolvedUom.getId(),
                        itemType,
                        taxabilityType,
                        vatRate,
                        null, // exciseType
                        null, // exciseRate
                        null, // exciseAmount
                        sellingPrice,
                        null, // wholesalePrice
                        null, // minSellingPrice
                        null, // secondaryUomId
                        null, // conversionFactor
                        purchasePrice,
                        null, // mrp
                        false, // isVatInclusive
                        isTracked,
                        false, // isBatchTracked
                        false, // isExpiryTracked
                        minStock,
                        reorderLevel,
                        null, // reorderQuantity
                        null, // isPurchasable
                        null, // isSellable
                        null, // isComponent
                        null, // isManufactured
                        openingQty,
                        openingCost,
                        openingWhId
                );

                try {
                    itemService.createItem(tenantId, companyId, request);
                    importedCount++;
                } catch (Exception ex) {
                    log.warn("Failed to import CSV item row {} (SKU: {}): {}", rowNumber, sku, ex.getMessage());
                    errors.add(new ItemImportResult.RowError(rowNumber, sku, nameEn, ex.getMessage()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse items CSV: {}", e.getMessage(), e);
            errors.add(new ItemImportResult.RowError(1, "", "", "Failed to read CSV stream: " + e.getMessage()));
        }

        return new ItemImportResult(totalRows, importedCount, errors.size(), errors);
    }

    private static String normalizeHeader(String header) {
        if (header == null) return "";
        return header.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static String getCol(List<String> row, Map<String, Integer> colIndex, String... possibleNames) {
        for (String name : possibleNames) {
            String norm = normalizeHeader(name);
            Integer idx = colIndex.get(norm);
            if (idx != null && idx < row.size()) {
                String val = row.get(idx);
                if (val != null) {
                    return val.trim();
                }
            }
        }
        return null;
    }

    private static ItemType parseItemType(String typeStr) {
        if (StringUtils.isBlank(typeStr)) return ItemType.GOODS;
        String upper = typeStr.trim().toUpperCase(Locale.ROOT);
        if (upper.equals("INVENTORY")) return ItemType.GOODS;
        try {
            return ItemType.valueOf(upper);
        } catch (IllegalArgumentException e) {
            return ItemType.GOODS;
        }
    }

    private static TaxabilityType parseTaxabilityType(String taxStr) {
        if (StringUtils.isBlank(taxStr)) return TaxabilityType.TAXABLE;
        String upper = taxStr.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("13") || upper.equals("TAXABLE")) return TaxabilityType.TAXABLE;
        if (upper.contains("EXEMPT")) return TaxabilityType.EXEMPT;
        if (upper.contains("ZERO")) return TaxabilityType.ZERO_RATED;
        return TaxabilityType.TAXABLE;
    }

    private static BigDecimal parseBigDecimal(String val, BigDecimal defaultVal) {
        if (StringUtils.isBlank(val)) return defaultVal;
        try {
            return new BigDecimal(val.trim().replace(",", ""));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static boolean parseBoolean(String val, boolean defaultVal) {
        if (StringUtils.isBlank(val)) return defaultVal;
        String s = val.trim().toLowerCase(Locale.ROOT);
        return s.equals("true") || s.equals("yes") || s.equals("1") || s.equals("y");
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (needsQuotes) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static List<List<String>> parseCsv(BufferedReader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        String line;
        StringBuilder field = new StringBuilder();
        List<String> currentRow = new ArrayList<>();
        boolean inQuotes = false;

        while ((line = reader.readLine()) != null) {
            // Strip potential UTF-8 BOM on first line
            if (rows.isEmpty() && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (inQuotes) {
                    if (c == '"') {
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            field.append('"');
                            i++; // skip escaped quote
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        field.append(c);
                    }
                } else {
                    if (c == '"') {
                        inQuotes = true;
                    } else if (c == ',') {
                        currentRow.add(field.toString().trim());
                        field.setLength(0);
                    } else {
                        field.append(c);
                    }
                }
            }

            if (inQuotes) {
                field.append('\n');
            } else {
                currentRow.add(field.toString().trim());
                field.setLength(0);
                if (!currentRow.isEmpty() && !(currentRow.size() == 1 && currentRow.get(0).isEmpty())) {
                    rows.add(currentRow);
                }
                currentRow = new ArrayList<>();
            }
        }
        if (!currentRow.isEmpty()) {
            currentRow.add(field.toString().trim());
            if (!(currentRow.size() == 1 && currentRow.get(0).isEmpty())) {
                rows.add(currentRow);
            }
        }
        return rows;
    }
}
