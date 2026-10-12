package com.erp.business.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins down the invariants {@link Warehouse} is responsible for.
 *
 * <p>The rules here are the ones that decide where incoming goods land. If the
 * marker and the flag ever disagreed, or an inactive warehouse could be made the
 * default, stock would be posted somewhere that cannot accept it and the failure
 * would only surface on the next delivery.
 */
class WarehouseTest {

    private static final Long TENANT = 1L;
    private static final Long COMPANY = 10L;

    private static Warehouse warehouse() {
        return Warehouse.create(TENANT, COMPANY, " cold ", " Cold Store ",
                WarehouseType.COLD_STORAGE, 7L, "  Ring Road ", " 9800000000 ");
    }

    @Nested
    @DisplayName("defaultFor()")
    class DefaultFor {

        @Test
        @DisplayName("produces an active, general-purpose default scoped to the company")
        void createsUsableDefault() {
            Warehouse created = Warehouse.defaultFor(TENANT, COMPANY, 7L);

            assertThat(created.getCode()).isEqualTo(Warehouse.DEFAULT_CODE);
            assertThat(created.getType()).isEqualTo(WarehouseType.GENERAL);
            assertThat(created.getIsActive()).isTrue();
            assertThat(created.getIsDefault()).isTrue();
            assertThat(created.getBranchId()).isEqualTo(7L);
            assertThat(created.getTenantId()).isEqualTo(TENANT);
            assertThat(created.getCompanyId()).isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("sets the marker, because that is what the unique constraint reads")
        void setsMarker() {
            // The marker and the flag are two columns describing one fact. The database
            // constraint is declared over the marker, so a default without one would
            // insert happily and two defaults would both be accepted.
            assertThat(Warehouse.defaultFor(TENANT, COMPANY, null).getDefaultMarker())
                    .isNotNull();
        }

        @Test
        @DisplayName("reuses a reserved code so two concurrent inserts collide rather than both winning")
        void reservedCodeIsStable() {
            assertThat(Warehouse.defaultFor(TENANT, COMPANY, null).getCode())
                    .isEqualTo(Warehouse.defaultFor(TENANT, COMPANY, 7L).getCode());
        }

        @Test
        @DisplayName("refuses a missing scope rather than building an unscoped row")
        void rejectsMissingScope() {
            assertThatThrownBy(() -> Warehouse.defaultFor(null, COMPANY, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Warehouse.defaultFor(TENANT, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("normalises the code to upper case and trims the name")
        void normalisesInput() {
            Warehouse created = warehouse();

            assertThat(created.getCode()).isEqualTo("COLD");
            assertThat(created.getName()).isEqualTo("Cold Store");
            assertThat(created.getAddress()).isEqualTo("Ring Road");
            assertThat(created.getPhone()).isEqualTo("9800000000");
        }

        @Test
        @DisplayName("starts active but never as the default")
        void startsActiveAndNotDefault() {
            Warehouse created = warehouse();

            assertThat(created.getIsActive()).isTrue();
            assertThat(created.getIsDefault()).isFalse();
            // Null, not false: the unique constraint over the marker has to admit many
            // non-default rows per company.
            assertThat(created.getDefaultMarker()).isNull();
        }

        @Test
        @DisplayName("leaves the branch unset when none is given")
        void branchIsOptional() {
            assertThat(Warehouse.create(TENANT, COMPANY, "MAIN", "Main", WarehouseType.GENERAL,
                    null, null, null).getBranchId()).isNull();
        }

        @Test
        @DisplayName("rejects a blank code, a blank name and a missing type")
        void rejectsIncompleteInput() {
            assertThatThrownBy(() -> Warehouse.create(TENANT, COMPANY, "  ", "Main",
                    WarehouseType.GENERAL, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Warehouse.create(TENANT, COMPANY, "M1", " ",
                    WarehouseType.GENERAL, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Warehouse.create(TENANT, COMPANY, "M1", "Main",
                    null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateDetails()")
    class UpdateDetails {

        @Test
        @DisplayName("leaves omitted fields untouched instead of clearing them")
        void partialUpdate() {
            Warehouse created = warehouse();

            created.updateDetails(null, "  Renamed Store ", null, null, null, null);

            assertThat(created.getName()).isEqualTo("Renamed Store");
            assertThat(created.getCode()).isEqualTo("COLD");
            assertThat(created.getType()).isEqualTo(WarehouseType.COLD_STORAGE);
            assertThat(created.getAddress()).isEqualTo("Ring Road");
            assertThat(created.getPhone()).isEqualTo("9800000000");
        }

        @Test
        @DisplayName("leaves the default marker alone even when the code changes")
        void doesNotDisturbDefaultMarker() {
            Warehouse created = Warehouse.defaultFor(TENANT, COMPANY, null);

            created.updateDetails("HEAD-OFFICE", "Head Office", null, null, null, null);

            assertThat(created.getIsDefault()).isTrue();
            assertThat(created.getDefaultMarker()).isNotNull();
        }
    }

    @Nested
    @DisplayName("default status")
    class DefaultStatus {

        @Test
        @DisplayName("marking sets both the flag and the marker")
        void markSetsBoth() {
            Warehouse created = warehouse();

            created.markAsDefault();

            assertThat(created.getIsDefault()).isTrue();
            assertThat(created.getDefaultMarker()).isNotNull();
        }

        @Test
        @DisplayName("clearing drops the marker so another warehouse can take the slot")
        void clearReleasesTheSlot() {
            Warehouse created = Warehouse.defaultFor(TENANT, COMPANY, null);

            created.clearDefault();

            assertThat(created.getIsDefault()).isFalse();
            assertThat(created.getDefaultMarker()).isNull();
        }

        @Test
        @DisplayName("refuses to promote an inactive warehouse")
        void refusesInactive() {
            // Otherwise the company is left with a default that nothing can be received
            // into, and the mistake only shows up on the next delivery.
            Warehouse created = warehouse();
            created.activate();
            created.deactivate();

            assertThatThrownBy(created::markAsDefault)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inactive");
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("refuses to switch off the company default")
        void refusesDefault() {
            Warehouse created = Warehouse.defaultFor(TENANT, COMPANY, null);

            assertThatThrownBy(created::deactivate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("default");
        }

        @Test
        @DisplayName("allows retiring an ordinary warehouse")
        void allowsNonDefault() {
            Warehouse created = warehouse();

            created.deactivate();

            assertThat(created.getIsActive()).isFalse();

            created.activate();

            assertThat(created.getIsActive()).isTrue();
        }
    }
}
