package com.erp.business.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins down the invariants {@link Unit} is responsible for.
 *
 * <p>These are the rules that would silently corrupt inventory arithmetic if
 * they regressed: a code that is not normalised breaks the per-company unique
 * lookup, and an out-of-range decimal scale breaks quantity rounding.
 */
class UnitTest {

    private static Unit unit(String name, String code, Integer decimalScale) {
        return Unit.create(1L, 10L, name, code, UnitDimension.MASS, "kg", decimalScale);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("normalises the code to upper case and trims the name")
        void normalisesInput() {
            Unit created = unit("  Kilogram ", " kg ", 2);

            assertThat(created.getCode()).isEqualTo("KG");
            assertThat(created.getName()).isEqualTo("Kilogram");
        }

        @Test
        @DisplayName("assigns the tenant and company scope and starts active")
        void assignsScope() {
            Unit created = unit("Kilogram", "KG", 2);

            assertThat(created.getTenantId()).isEqualTo(1L);
            assertThat(created.getCompanyId()).isEqualTo(10L);
            assertThat(created.getIsActive()).isTrue();
            assertThat(created.isOwnedBy(1L, 10L)).isTrue();
            assertThat(created.isOwnedBy(1L, 11L)).isFalse();
        }

        @Test
        @DisplayName("defaults a null decimal scale to 0")
        void defaultsDecimalScale() {
            assertThat(unit("Pieces", "PCS", null).getDecimalScale()).isZero();
        }

        @Test
        @DisplayName("treats a blank symbol as absent rather than an empty string")
        void blankSymbolBecomesNull() {
            assertThat(unit("Kilogram", "KG", 2).getSymbol()).isEqualTo("kg");
            assertThat(Unit.create(1L, 10L, "Pieces", "PCS", UnitDimension.COUNT, "   ", 0).getSymbol())
                    .isNull();
        }

        @Test
        @DisplayName("rejects a blank or missing code")
        void rejectsBlankCode() {
            assertThatThrownBy(() -> unit("Kilogram", "  ", 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must not be blank");

            assertThatThrownBy(() -> unit("Kilogram", null, 2))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a missing dimension")
        void rejectsMissingDimension() {
            assertThatThrownBy(() -> Unit.create(1L, 10L, "Kilogram", "KG", null, "kg", 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dimension is required");
        }

        @Test
        @DisplayName("rejects a negative or oversized decimal scale")
        void rejectsOutOfRangeDecimalScale() {
            assertThatThrownBy(() -> unit("Pieces", "PCS", -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");

            assertThatThrownBy(() -> unit("Pieces", "PCS", Unit.MAX_DECIMAL_SCALE + 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not exceed");
        }

        @Test
        @DisplayName("rejects a missing tenant or company scope")
        void rejectsMissingScope() {
            assertThatThrownBy(() -> Unit.create(null, 10L, "Kilogram", "KG", UnitDimension.MASS, "kg", 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tenant ID");

            assertThatThrownBy(() -> Unit.create(1L, null, "Kilogram", "KG", UnitDimension.MASS, "kg", 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company ID");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("leaves omitted required fields untouched")
        void partialUpdateLeavesRequiredFieldsAlone() {
            Unit created = unit("Kilogram", "KG", 2);

            created.update(null, null, null, "kgs", null);

            assertThat(created.getName()).isEqualTo("Kilogram");
            assertThat(created.getCode()).isEqualTo("KG");
            assertThat(created.getDimension()).isEqualTo(UnitDimension.MASS);
            assertThat(created.getDecimalScale()).isEqualTo(2);
            assertThat(created.getSymbol()).isEqualTo("kgs");
        }

        @Test
        @DisplayName("clears the symbol when it is sent as blank")
        void blankSymbolClearsIt() {
            Unit created = unit("Kilogram", "KG", 2);

            created.update(null, null, null, "   ", null);

            assertThat(created.getSymbol()).isNull();
        }

        @Test
        @DisplayName("re-normalises a changed code and validates the new one")
        void reNormalisesChangedCode() {
            Unit created = unit("Kilogram", "KG", 2);

            created.update(null, " gram ", null, null, null);

            assertThat(created.getCode()).isEqualTo("GRAM");
        }
    }

    @Nested
    @DisplayName("activate() / deactivate()")
    class Lifecycle {

        @Test
        @DisplayName("round-trips between active and inactive")
        void roundTrips() {
            Unit created = unit("Kilogram", "KG", 2);

            created.deactivate();
            assertThat(created.getIsActive()).isFalse();

            created.activate();
            assertThat(created.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("fails loudly on a redundant transition")
        void rejectsRedundantTransition() {
            Unit created = unit("Kilogram", "KG", 2);

            assertThatThrownBy(created::activate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already active");
        }
    }

    @Test
    @DisplayName("reports fractional units from the decimal scale")
    void reportsFractional() {
        assertThat(unit("Pieces", "PCS", 0).isFractional()).isFalse();
        assertThat(unit("Kilogram", "KG", 2).isFractional()).isTrue();
    }
}
