package com.erp.business.pos.config;

import com.erp.business.pos.audit.FiscalAuditLogRepository;
import com.erp.business.pos.audit.inspector.AuditorSecurityConfig;
import com.erp.platform.security.filter.PlatformJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the POS Service.
 *
 * <p>Validates the access token minted by the Identity Service with the shared
 * {@link PlatformJwtAuthenticationFilter}; it never issues tokens. CORS is left
 * to the API Gateway, the single CORS authority.
 *
 * <p>IRD Tax Auditors ({@code ROLE_TAX_AUDITOR}) are granted strictly read-only
 * access: every non-GET request is rejected. Each auditor GET is logged to
 * {@code fiscal_audit_logs}.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Order(0)
public class SecurityConfig {

    private final PlatformJwtAuthenticationFilter platformJwtAuthenticationFilter;
    private final FiscalAuditLogRepository auditLogRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                // IRD Tax Auditor read-only gate: auditors may only issue GET.
                .requestMatchers(req -> isAuditor() && !HttpMethod.GET.matches(req.getMethod()))
                    .denyAll()
                .requestMatchers("/api/v1/pos/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(platformJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new AuditorSecurityConfig.AuditorSessionAuditFilter(auditLogRepository),
                    PlatformJwtAuthenticationFilter.class);

        return http.build();
    }

    private boolean isAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> AuditorSecurityConfig.ROLE_TAX_AUDITOR.equals(a.getAuthority()));
    }
}
