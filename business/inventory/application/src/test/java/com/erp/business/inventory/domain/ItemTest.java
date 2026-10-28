package com.erp.business.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Rules that {@link Item} enforces on itself.
 *
 * <p>Before these lived in the entity, the service called a public setter per field and
 * nothing stopped a caller from storing a negative price or an item that claimed to be
 * VAT-exempt at 13%. These tests pin the behaviour that now makes that impossible.
 */
class ItemTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");
    private static final Unit UOM = Unit.create(TENANT_ID, COMPANY_ID, "Pieces", "PCS",
            UnitDimension.COUNT, "pcs", 0);

    private static Item newItem() {
        return Item.create(TENANT_ID, COMPANY_ID, "SKU-1", "Test Item", UOM, PRICE, null);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("normalises the SKU to upper case and trims the name")
        void normalisesTextFields() {
            Item item = Item.create(TENANT_ID, COMPANY_ID, "  sku-1 ", "  Widget  ", UOM,
                    PRICE, null);

            assertThat(item.getSku()).isEqualTo("SKU-1");
            assertThat(item.getNameEn()).isEqualTo("Widget");
        }

        @Test
        @DisplayName("rejects a non-positive tenant or company id")
        void rejectsInvalidScope() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Item.create(0L, COMPANY_ID, "SKU-1", "Item", UOM, PRICE, null));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Item.create(TENANT_ID, null, "SKU-1", "Item", UOM, PRICE, null));
        }

        @Test
        @DisplayName("requires a SKU, a name, a UOM and a selling price")
        void rejectsMissingRequiredFields() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Item.create(TENANT_ID, COMPANY_ID, "  ", "Item", UOM, PRICE, null));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Item.create(TENANT_ID, COMPANY_ID, "SKU-1", "  ", UOM, PRICE, null));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Item.create(TENANT_ID, COMPANY_ID, "SKU-1", "Item", null, PRICE, null))
                    .withMessageContaining("UOM");
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Item.create(TENANT_ID, COMPANY_ID, "SKU-1", "Item", UOM, null, null))
                    .withMessageContaining("Selling price");
        }

        @Test
        @DisplayName("rejects a negative selling price")
        void rejectsNegativeSellingPrice() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Item.create(TENANT_ID, COMPANY_ID, "SKU-1", "Item", UOM,
                            new BigDecimal("-1.00"), null))
                    .withMessageContaining("non-negative");
        }

        @Test
        @DisplayName("defaults to taxable at the standard VAT rate")
        void appliesDefaultTax() {
            Item item = newItem();

            assertThat(item.getTaxabilityType()).isEqualTo(TaxabilityType.TAXABLE);
            assertThat(item.getVatRate()).isEqualByComparingTo(Item.DEFAULT_VAT_RATE);
            assertThat(item.getExciseType()).isEqualTo(ExciseType.NONE);
        }
    }

    @Nested
    @DisplayName("applyChanges")
    class ApplyChanges {

        @Test
        @DisplayName("a null field leaves the current value untouched")
        void nullFieldsAreIgnored() {
            Item item = newItem();

            item.applyChanges(Item.ItemChanges.builder().nameEn("Renamed").build());

            assertThat(item.getNameEn()).isEqualTo("Renamed");
            assertThat(item.getSku()).isEqualTo("SKU-1");
            assertThat(item.getSellingPrice()).isEqualByComparingTo(PRICE);
        }

        @Test
        @DisplayName("a blank optional string is stored as null rather than an empty string")
        void blankStringsBecomeNull() {
            Item item = newItem();

            item.applyChanges(Item.ItemChanges.builder()
                    .hsCode("   ")
                    .nameNp(" ")
                    .descriptionEn("")
                    .build());

            assertThat(item.getHsCode()).isNull();
            assertThat(item.getNameNp()).isNull();
            assertThat(item.getDescriptionEn()).isNull();
        }

        @Test
        @DisplayName("rejects a negative price or stock level")
        void rejectsNegativeAmounts() {
            Item item = newItem();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .sellingPrice(new BigDecimal("-0.01")).build()));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .minStockLevel(new BigDecimal("-1")).build()));
        }

        @Test
        @DisplayName("rejects a tax rate above 100%")
        void rejectsAbsurdTaxRate() {
            Item item = newItem();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .vatRate(new BigDecimal("150.00")).build()));
        }

        @Test
        @DisplayName("rejects a value longer than the column allows")
        void rejectsOverlongValues() {
            Item item = newItem();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .sku("X".repeat(101)).build()));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .barcode("9".repeat(101)).build()));
        }

        @Test
        @DisplayName("a rejected change leaves the item exactly as it was")
        void rejectedChangeIsAtomic() {
            Item item = newItem();
            BigDecimal originalPrice = item.getSellingPrice();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .nameEn("Renamed")
                            .sellingPrice(new BigDecimal("-5"))
                            .build()));

            assertThat(item.getNameEn()).isEqualTo("Test Item");
            assertThat(item.getSellingPrice()).isEqualByComparingTo(originalPrice);
        }
    }

    @Nested
    @DisplayName("tax consistency")
    class TaxConsistency {

        @Test
        @DisplayName("an exempt item cannot carry a VAT rate")
        void exemptItemMustBeZeroRated() {
            Item item = newItem();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .taxabilityType(TaxabilityType.EXEMPT)
                            .vatRate(new BigDecimal("13.00"))
                            .build()))
                    .withMessageContaining("EXEMPT");
        }

        @Test
        @DisplayName("switching to exempt without sending a rate derives a zero rate")
        void switchingToExemptDerivesZeroRate() {
            Item item = newItem();
            assertThat(item.getVatRate()).isEqualByComparingTo("13.00");

            item.applyChanges(Item.ItemChanges.builder()
                    .taxabilityType(TaxabilityType.ZERO_RATED)
                    .build());

            assertThat(item.getVatRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("switching back to taxable restores the standard rate")
        void switchingBackToTaxableRestoresStandardRate() {
            Item item = newItem();
            item.applyChanges(Item.ItemChanges.builder()
                    .taxabilityType(TaxabilityType.EXEMPT)
                    .vatRate(BigDecimal.ZERO)
                    .build());

            item.applyChanges(Item.ItemChanges.builder()
                    .taxabilityType(TaxabilityType.TAXABLE)
                    .build());

            assertThat(item.getVatRate()).isEqualByComparingTo(Item.DEFAULT_VAT_RATE);
        }
    }

    @Nested
    @DisplayName("excise consistency")
    class ExciseConsistency {

        @Test
        @DisplayName("percentage excise requires a positive rate")
        void percentageExciseRequiresRate() {
            Item item = newItem();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .exciseType(ExciseType.PERCENTAGE)
                            .build()));
        }

        @Test
        @DisplayName("specific excise requires a positive fixed amount")
        void specificExciseRequiresAmount() {
            Item item = newItem();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .exciseType(ExciseType.SPECIFIC_AMOUNT)
                            .build()));
        }

        @Test
        @DisplayName("no excise must not leave a residual rate behind")
        void noExciseRejectsResidualRate() {
            Item item = newItem();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> item.applyChanges(Item.ItemChanges.builder()
                            .exciseType(ExciseType.NONE)
                            .exciseRate(new BigDecimal("5.00"))
                            .build()))
                    .withMessageContaining("no excise");
        }

        @Test
        @DisplayName("a consistent percentage excise is accepted")
        void acceptsConsistentExcise() {
            Item item = newItem();

            item.applyChanges(Item.ItemChanges.builder()
                    .exciseType(ExciseType.PERCENTAGE)
                    .exciseRate(new BigDecimal("5.00"))
                    .build());

            assertThat(item.getExciseType()).isEqualTo(ExciseType.PERCENTAGE);
            assertThat(item.getExciseRate()).isEqualByComparingTo("5.00");
        }
    }

    @Nested
    @DisplayName("activation")
    class Activation {

        @Test
        @DisplayName("an item starts active and toggles both ways")
        void togglesActiveState() {
            Item item = newItem();
            assertThat(item.getIsActive()).isTrue();

            item.deactivate();
            assertThat(item.getIsActive()).isFalse();

            item.activate();
            assertThat(item.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("activating twice or deactivating twice is rejected")
        void rejectsRedundantTransitions() {
            Item item = newItem();

            assertThatIllegalStateException().isThrownBy(item::activate);

            item.deactivate();
            assertThatIllegalStateException().isThrownBy(item::deactivate);
        }
    }
}
