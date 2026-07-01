package com.erp.platform.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Status}.
 *
 * @since 1.0.0
 */
class StatusTest {

    @Test
    void active_shouldBeActive() {
        assertThat(Status.ACTIVE.isActive()).isTrue();
        assertThat(Status.ACTIVE.isInactive()).isFalse();
    }

    @Test
    void inactive_shouldBeInactive() {
        assertThat(Status.INACTIVE.isInactive()).isTrue();
        assertThat(Status.INACTIVE.isActive()).isFalse();
    }

    @Test
    void pending_shouldBePending() {
        assertThat(Status.PENDING.isPending()).isTrue();
    }

    @Test
    void suspended_shouldBeSuspended() {
        assertThat(Status.SUSPENDED.isSuspended()).isTrue();
    }

    @Test
    void archived_shouldBeArchived() {
        assertThat(Status.ARCHIVED.isArchived()).isTrue();
    }

    @Test
    void deleted_shouldBeDeleted() {
        assertThat(Status.DELETED.isDeleted()).isTrue();
    }

    @Test
    void deleted_shouldBeTerminal() {
        assertThat(Status.DELETED.isTerminal()).isTrue();
    }

    @Test
    void archived_shouldBeTerminal() {
        assertThat(Status.ARCHIVED.isTerminal()).isTrue();
    }

    @Test
    void active_shouldNotBeTerminal() {
        assertThat(Status.ACTIVE.isTerminal()).isFalse();
    }

    @Test
    void getDescription_shouldReturnDescription() {
        assertThat(Status.ACTIVE.getDescription()).isEqualTo("Active");
        assertThat(Status.INACTIVE.getDescription()).isEqualTo("Inactive");
    }

    @Test
    void values_shouldContainAllStatuses() {
        assertThat(Status.values()).containsExactly(
            Status.ACTIVE,
            Status.INACTIVE,
            Status.PENDING,
            Status.SUSPENDED,
            Status.ARCHIVED,
            Status.DELETED
        );
    }
}
