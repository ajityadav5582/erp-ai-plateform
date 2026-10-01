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

import java.math.BigDecimal;
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

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemServiceImpl(itemRepository, categoryRepository, unitRepository, itemMapper);
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

    private static UpdateItemRequest renameRequest() {
        // UpdateItemRequest is a 25-component record, so only the fields under test are
        // named; every other component is left null, which means "leave unchanged".
        return new UpdateItemRequest(
                null, null, null,
                "Renamed",
                null, null, null,
                null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null, null, null, null, null);
    }

    private static CreateItemRequest createRequest() {
        return new CreateItemRequest(
                "sku-1", null, null,
                "Widget", null, null, null,
                null, 1L,
                null, null, null,
                null, null, null,
                new BigDecimal("100.00"),
                null, null, null, null, null, null,
                null, null, null);
    }
}
