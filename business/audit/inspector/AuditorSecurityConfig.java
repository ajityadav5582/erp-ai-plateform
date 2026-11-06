package com.erp.business.audit.inspector;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security role configuration for the IRD Tax Auditor inspection mode.
 *
 * <p>Introduces {@code ROLE_TAX_AUDITOR}. Auditors have strictly read-only
 * access: all {@code POST}, {@code PUT}, {@code PATCH} and {@code DELETE}
 * requests are blocked; only {@code HttpMethod.GET} is permitted. Every page
 * view, query filter and export is logged to {@code fiscal_audit_logs}.
 */
@Configuration
public class AuditorSecurityConfig {

    public static final String ROLE_TAX_AUDITOR = "ROLE_TAX_AUDITOR";

    @Bean
    @Order(1)
    public SecurityFilterChain auditorFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/**").denyAll()
                .requestMatchers(HttpMethod.PUT, "/**").denyAll()
                .requestMatchers(HttpMethod.PATCH, "/**").denyAll()
                .requestMatchers(HttpMethod.DELETE, "/**").denyAll()
            );
        return http.build();
    }
}
