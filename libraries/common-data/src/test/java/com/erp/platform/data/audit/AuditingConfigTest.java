package com.erp.platform.data.audit;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AuditingConfig}.
 *
 * @since 1.0.0
 */
class AuditingConfigTest {

    @Test
    void getCurrentAuditor_shouldReturnSystemWhenNoContext() {
        AuditingConfig auditingConfig = new AuditingConfig();

        Optional<String> auditor = auditingConfig.getCurrentAuditor();

        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("system");
    }
}
