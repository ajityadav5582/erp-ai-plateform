package com.erp.business.inventory.items;

import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.business.inventory.infrastructure.persistence.CategoryRepository;
import com.erp.business.inventory.infrastructure.persistence.ItemRepository;
import com.erp.business.inventory.infrastructure.persistence.UnitRepository;
import com.erp.business.inventory.items.dto.CreateItemRequest;
import com.erp.business.inventory.items.dto.ItemListResponse;
import com.erp.business.inventory.items.dto.UpdateItemRequest;
import com.erp.business.inventory.items.exception.DuplicateItemSkuException;
import com.erp.business.inventory.items.exception.ItemNotFoundException;
import com.erp.business.inventory.items.mapper.ItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Company isolation for {@link ItemServiceImpl}.
 *
 * <p>Every company in a tenant shares one {@code tenantId}, so a lookup that filters on
 * tenant alone hands any authenticated user another company's item, unit or category by
 * id alone. These tests pin the company predicate on each of those paths. They are unit
 * tests with mocked repositories: what matters here is the <em>argument</em> the service
 * passes to the repository, which a mock captures exactly.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceImplCompanyScopingTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final Long OTHER_COMPANY_ID = 99L;
    private static final Long ITEM_ID = 500L;

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private com.erp.business.inventory.stock.StockMovementService stockMovementService;
    @Mock
    private com.erp.business.inventory.infrastructure.persistence.StockMovementRepository stockMovementRepository;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemServiceImpl(itemRepository, categoryRepository, unitRepository, itemMapper, stockMovementService, stockMovementRepository);
    }

    @Test
    @DisplayName("reading an item loads it by tenant and company, never by tenant alone")
    void getItemByIdIsCompanyScoped() {
        when(itemRepository.findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, ITEM_ID))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> service.getItemById(TENANT_ID, COMPANY_ID, ITEM_ID));

        // The repository has no tenant-only finder for items, and none was reached:
        // the only interaction is the company-scoped one that missed.
        verify(itemRepository).findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, ITEM_ID);
    }

    @Test
    @DisplayName("an item belonging to another company reports not found, not forbidden")
    void otherCompanyItemLooksMissing() {
        // The row exists, but not under this companyId, so the scoped finder misses.
        when(itemRepository.findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, ITEM_ID))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> service.activateItem(TENANT_ID, COMPANY_ID, ITEM_ID))
                .satisfies(ex -> assertThat(ex.getStatus().value()).isEqualTo(404));

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updating tracking flags on an item with existing stock movements throws IllegalStateException")
    void cannotAlterTrackingFlagsWhenStockMovementsExist() {
        Item existingItem = mock(Item.class);
        when(existingItem.getSku()).thenReturn("SKU-1");
        when(existingItem.getIsInventoryTracked()).thenReturn(true);

        when(itemRepository.findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, ITEM_ID))
                .thenReturn(Optional.of(existingItem));
        when(stockMovementRepository.existsByTenantIdAndCompanyIdAndItemId(TENANT_ID, COMPANY_ID, ITEM_ID))
                .thenReturn(true);

        UpdateItemRequest requestWithTrackingChange = new UpdateItemRequest(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null,
                false, // attempting to turn off inventory tracking
                null, null, null, null, null, null, null, null, null
        );

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> service.updateItem(TENANT_ID, COMPANY_ID, ITEM_ID, requestWithTrackingChange))
                .withMessageContaining("Cannot alter inventory, batch, or expiry tracking flags for item [SKU-1] because stock movements already exist in ledger.");

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updating loads the item scoped to the caller's company")
    void updateItemIsCompanyScoped() {
        when(itemRepository.findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, ITEM_ID))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> service.updateItem(TENANT_ID, COMPANY_ID, ITEM_ID, renameRequest()));

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleting loads the item scoped to the caller's company")
    void deleteItemIsCompanyScoped() {
        when(itemRepository.findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, ITEM_ID))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ItemNotFoundException.class)
                .isThrownBy(() -> service.deleteItem(TENANT_ID, COMPANY_ID, ITEM_ID));

        verify(itemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("the duplicate SKU check runs against the caller's company")
    void duplicateSkuCheckUsesCallerCompany() {
        when(itemRepository.existsByTenantIdAndCompanyIdAndSku(TENANT_ID, COMPANY_ID, "SKU-1"))
                .thenReturn(true);

        assertThatExceptionOfType(DuplicateItemSkuException.class)
                .isThrownBy(() -> service.createItem(TENANT_ID, COMPANY_ID, createRequest()));

        verify(itemRepository).existsByTenantIdAndCompanyIdAndSku(TENANT_ID, COMPANY_ID, "SKU-1");
        verify(itemRepository, never())
                .existsByTenantIdAndCompanyIdAndSku(eq(TENANT_ID), eq(OTHER_COMPANY_ID), any());
    }

    @Test
    @DisplayName("listings are keyed on the caller's company, not the tenant alone")
    void listingsAreCompanyScoped() {
        Pageable pageable = PageRequest.of(0, 20);
        when(itemRepository.findByTenantIdAndCompanyId(TENANT_ID, COMPANY_ID, pageable))
                .thenReturn(Page.empty());

        Page<ItemListResponse> page = service.listItems(TENANT_ID, COMPANY_ID, pageable);

        assertThat(page.getContent()).isEmpty();
        verify(itemRepository).findByTenantIdAndCompanyId(TENANT_ID, COMPANY_ID, pageable);
    }

    @Test
    @DisplayName("active items are filtered by the caller's company")
    void activeItemsAreCompanyScoped() {
        Pageable pageable = PageRequest.of(0, 20);
        when(itemRepository.findByTenantIdAndCompanyIdAndIsActiveTrue(TENANT_ID, COMPANY_ID, pageable))
                .thenReturn(Page.empty());

        assertThat(service.listActiveItems(TENANT_ID, COMPANY_ID, pageable).getContent()).isEmpty();

        verify(itemRepository).findByTenantIdAndCompanyIdAndIsActiveTrue(TENANT_ID, COMPANY_ID, pageable);
    }

    @Test
    @DisplayName("items by taxability are filtered by the caller's company")
    void itemsByTaxabilityAreCompanyScoped() {
        Pageable pageable = PageRequest.of(0, 20);
        when(itemRepository.findByTenantIdAndCompanyIdAndTaxabilityType(
                TENANT_ID, COMPANY_ID, TaxabilityType.EXEMPT, pageable))
                .thenReturn(Page.empty());

        assertThat(service.listItemsByTaxability(TENANT_ID, COMPANY_ID, TaxabilityType.EXEMPT, pageable)
                .getContent()).isEmpty();

        verify(itemRepository).findByTenantIdAndCompanyIdAndTaxabilityType(
                TENANT_ID, COMPANY_ID, TaxabilityType.EXEMPT, pageable);
    }

    @Test
    @DisplayName("search is keyed on the caller's company")
    void searchIsCompanyScoped() {
        Pageable pageable = PageRequest.of(0, 20);
        when(itemRepository.searchItems(TENANT_ID, COMPANY_ID, "widget", pageable))
                .thenReturn(Page.empty());

        assertThat(service.searchItems(TENANT_ID, COMPANY_ID, "widget", pageable).getContent())
                .isEmpty();

        verify(itemRepository).searchItems(TENANT_ID, COMPANY_ID, "widget", pageable);
    }

    @Test
    @DisplayName("a hit is mapped, not re-fetched through any other finder")
    void successfulLoadMapsTheScopedResult() {
        Item item = mock(Item.class);
        when(itemRepository.findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, ITEM_ID))
                .thenReturn(Optional.of(item));

        service.getItemById(TENANT_ID, COMPANY_ID, ITEM_ID);

        verify(itemMapper).toResponse(item);
        // Exactly one repository interaction: the scoped load and nothing else.
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    @DisplayName("creating item with opening stock posts initial stock movement to opening warehouse")
    void createItemWithOpeningStockPostsStockMovement() {
        com.erp.business.inventory.domain.Unit unit = mock(com.erp.business.inventory.domain.Unit.class);
        when(unit.getId()).thenReturn(12L);
        when(unitRepository.findByTenantIdAndCompanyIdAndId(TENANT_ID, COMPANY_ID, 12L)).thenReturn(Optional.of(unit));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item it = inv.getArgument(0);
            ReflectionTestUtils.setField(it, "id", 501L);
            return it;
        });

        CreateItemRequest request = new CreateItemRequest(
                "SKU-OPEN-1", "BAR-01", "HS-1", "English Item", "Nepali Item",
                "Desc En", "Desc Np", null, 12L, com.erp.business.inventory.domain.ItemType.GOODS,
                TaxabilityType.TAXABLE, BigDecimal.valueOf(13), com.erp.business.inventory.domain.ExciseType.NONE,
                null, null, BigDecimal.valueOf(150), null, null, null, null, null, BigDecimal.valueOf(160),
                false, true, false, false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true, true, false, false,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), 77L
        );

        service.createItem(TENANT_ID, COMPANY_ID, request);

        verify(stockMovementService).postReceipt(
                eq(TENANT_ID), eq(COMPANY_ID), eq(77L), eq(501L), eq(12L),
                eq(BigDecimal.valueOf(50)), eq("OPENING_BALANCE"), eq(501L),
                eq("OB-SKU-OPEN-1"), any(Instant.class), eq("SYSTEM"), any(String.class)
        );
    }

    private static UpdateItemRequest renameRequest() {
        return new UpdateItemRequest(
                null, null, null,
                "Renamed",
                null, null, null,
                null, null,
                null, null, null,
                null, null, null,
                null, null, null, null, null,
                null, null,
                null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private static CreateItemRequest createRequest() {
        return new CreateItemRequest(
                "sku-1", null, null,
                "Widget", null, null, null,
                null, 1L,
                null, null, null,
                null, null, null,
                new BigDecimal("100.00"),
                null, null, null, null,
                null, null,
                null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null);
    }
}
