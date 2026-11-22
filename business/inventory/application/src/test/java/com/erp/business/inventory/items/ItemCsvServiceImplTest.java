package com.erp.business.inventory.items;

import com.erp.business.inventory.domain.Category;
import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.domain.ItemType;
import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.business.inventory.domain.Unit;
import com.erp.business.inventory.domain.UnitDimension;
import com.erp.business.inventory.domain.Warehouse;
import com.erp.business.inventory.infrastructure.persistence.CategoryRepository;
import com.erp.business.inventory.infrastructure.persistence.ItemRepository;
import com.erp.business.inventory.infrastructure.persistence.UnitRepository;
import com.erp.business.inventory.infrastructure.persistence.WarehouseRepository;
import com.erp.business.inventory.items.dto.CreateItemRequest;
import com.erp.business.inventory.items.dto.ItemImportResult;
import com.erp.business.inventory.items.dto.ItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemCsvServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private ItemService itemService;

    private ItemCsvServiceImpl csvService;

    private final Long tenantId = 1L;
    private final Long companyId = 100L;

    @BeforeEach
    void setUp() {
        csvService = new ItemCsvServiceImpl(
                itemRepository,
                unitRepository,
                categoryRepository,
                warehouseRepository,
                itemService
        );
    }

    @Test
    @DisplayName("generateTemplateCsv returns valid CSV bytes with UTF-8 BOM and headers")
    void generateTemplateCsv() {
        byte[] csv = csvService.generateTemplateCsv();
        assertThat(csv).isNotNull().isNotEmpty();

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("SKU,Name (En),Name (Np),UOM Code");
        assertThat(content).contains("TAP-BR-01");
    }

    @Test
    @DisplayName("exportItemsCsv correctly formats item rows and handles special characters")
    void exportItemsCsv() {
        Unit unit = mock(Unit.class);
        when(unit.getCode()).thenReturn("PCS");

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Plumbing, Hardware");

        Item item = mock(Item.class);
        when(item.getSku()).thenReturn("ITM-01");
        when(item.getNameEn()).thenReturn("Brass Tap 1/2\"");
        when(item.getNameNp()).thenReturn("धारा");
        when(item.getUom()).thenReturn(unit);
        when(item.getCategory()).thenReturn(category);
        when(item.getItemType()).thenReturn(ItemType.GOODS);
        when(item.getTaxabilityType()).thenReturn(TaxabilityType.TAXABLE);
        when(item.getVatRate()).thenReturn(new BigDecimal("13.00"));
        when(item.getSellingPrice()).thenReturn(new BigDecimal("500.00"));
        when(item.getPurchasePrice()).thenReturn(new BigDecimal("400.00"));
        when(item.getIsInventoryTracked()).thenReturn(true);
        when(item.getBarcode()).thenReturn("890123");

        when(itemRepository.findByTenantIdAndCompanyIdOrderBySkuAsc(tenantId, companyId))
                .thenReturn(List.of(item));

        byte[] exported = csvService.exportItemsCsv(tenantId, companyId);
        String text = new String(exported, StandardCharsets.UTF_8);

        assertThat(text).contains("ITM-01");
        // Escaped quotes in name: "Brass Tap 1/2"""
        assertThat(text).contains("\"Brass Tap 1/2\"\"\"");
        // Escaped comma in category: "Plumbing, Hardware"
        assertThat(text).contains("\"Plumbing, Hardware\"");
        assertThat(text).contains("PCS");
    }

    @Test
    @DisplayName("importItemsCsv imports valid rows and reports row-level errors without throwing")
    void importItemsCsv() {
        Unit uomPcs = mock(Unit.class);
        when(uomPcs.getId()).thenReturn(10L);
        when(uomPcs.getCode()).thenReturn("PCS");
        when(uomPcs.getName()).thenReturn("Pieces");

        when(unitRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(tenantId, companyId))
                .thenReturn(List.of(uomPcs));

        Warehouse wh = mock(Warehouse.class);
        when(wh.getId()).thenReturn(5L);
        when(warehouseRepository.findDefaultWarehouse(tenantId, companyId)).thenReturn(Optional.of(wh));

        String csvData = "SKU,Name (En),Name (Np),UOM Code,Selling Price,Purchase Price,Opening Quantity\n" +
                "SKU-001,Kitchen Sink,,PCS,4500,3200,10\n" +
                ",Missing Name,,PCS,100,50,\n" +
                "SKU-003,Bad Unit Item,,INVALID_UOM,100,50,\n";

        ItemResponse mockResponse = mock(ItemResponse.class);
        when(itemService.createItem(eq(tenantId), eq(companyId), any(CreateItemRequest.class)))
                .thenReturn(mockResponse);

        ItemImportResult result = csvService.importItemsCsv(tenantId, companyId,
                new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.importedCount()).isEqualTo(2); // Row 1 and Row 2 have name
        assertThat(result.failedCount()).isEqualTo(1); // Row 3 had invalid UOM

        ArgumentCaptor<CreateItemRequest> captor = ArgumentCaptor.forClass(CreateItemRequest.class);
        verify(itemService, org.mockito.Mockito.atLeastOnce()).createItem(eq(tenantId), eq(companyId), captor.capture());

        CreateItemRequest firstReq = captor.getAllValues().get(0);
        assertThat(firstReq.sku()).isEqualTo("SKU-001");
        assertThat(firstReq.nameEn()).isEqualTo("Kitchen Sink");
        assertThat(firstReq.uomId()).isEqualTo(10L);
        assertThat(firstReq.openingQuantity()).isEqualTo(new BigDecimal("10"));
        assertThat(firstReq.openingWarehouseId()).isEqualTo(5L);
    }
}

