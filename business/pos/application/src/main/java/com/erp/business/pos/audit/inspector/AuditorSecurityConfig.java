package com.erp.business.pos.audit.inspector;

import com.erp.business.pos.audit.FiscalAuditLogRepository;
import com.erp.business.pos.audit.FiscalAuditLog;
import com.erp.platform.security.filter.PlatformJwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Security configuration for the IRD Tax Auditor inspection mode.
 *
 * <p>Auditors authenticate with the {@code ROLE_TAX_AUDITOR} role and are granted
 * strictly read-only access: every {@code POST}, {@code PUT}, {@code PATCH} and
 * {@code DELETE} request is rejected with {@code 403 Forbidden}. Only
 * {@code HttpMethod.GET} is permitted. Each page view, query filter and export
 * executed by an auditor is recorded in {@code fiscal_audit_logs}.
 */
@Configuration
public class AuditorSecurityConfig {

    public static final String ROLE_TAX_AUDITOR = "ROLE_TAX_AUDITOR";

    @Bean
    @Order(1)
    public SecurityFilterChain auditorFilterChain(HttpSecurity http,
                                                 PlatformJwtAuthenticationFilter jwtFilter,
                                                 FiscalAuditLogRepository auditLogRepository) throws Exception {
        http
            .securityMatcher("/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auditors may only read.
                .requestMatchers(req -> isAuditor() && !HttpMethod.GET.matches(req.getMethod()))
                    .denyAll()
                // Everyone else (and auditors on GET) must be authenticated.
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new AuditorSessionAuditFilter(auditLogRepository), PlatformJwtAuthenticationFilter.class);

        return http.build();
    }

    private boolean isAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_TAX_AUDITOR.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Records every GET request performed by an auditor into {@code fiscal_audit_logs}.
     */
    public static class AuditorSessionAuditFilter extends OncePerRequestFilter {

        private final FiscalAuditLogRepository auditLogRepository;

        public AuditorSessionAuditFilter(FiscalAuditLogRepository auditLogRepository) {
            this.auditLogRepository = auditLogRepository;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuditor = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> ROLE_TAX_AUDITOR.equals(a.getAuthority()));

            if (isAuditor && HttpMethod.GET.matches(request.getMethod())) {
                try {
                    FiscalAuditLog log = new FiscalAuditLog();
                    log.setTenantId(resolveTenantId(authentication));
                    log.setCompanyId(resolveCompanyId(request));
                    log.setActionType("AUDITOR_PAGE_VIEW");
                    log.setEntityName("AUDITOR_PORTAL");
                    log.setEntityId(request.getRequestURI());
                    log.setUserName(authentication.getName());
                    log.setDetailsJson("{\"method\":\"GET\",\"query\":\"" + escape(request.getQueryString()) + "\"}");
                    log.setIpAddress(request.getRemoteAddr());
                    log.setCreatedAt(OffsetDateTime.now());
                    auditLogRepository.save(log);
                } catch (Exception ignored) {
                    // Auditing must never break the request.
                }
            }

            filterChain.doFilter(request, response);
        }

        private Long resolveTenantId(Authentication authentication) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.erp.platform.security.principal.TenantPrincipal tp) {
                return tp.tenantId();
            }
            return null;
        }

        private Long resolveCompanyId(HttpServletRequest request) {
            String header = request.getHeader("X-Company-Id");
            if (header == null || header.isBlank()) {
                return null;
            }
            try {
                return Long.parseLong(header);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String escape(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("\"", "\\\"");
        }
    }
}
