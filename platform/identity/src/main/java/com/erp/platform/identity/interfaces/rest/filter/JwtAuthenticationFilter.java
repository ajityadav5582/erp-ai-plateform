package com.erp.platform.identity.interfaces.rest.filter;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.identity.application.security.AccessTokenClaims;
import com.erp.platform.identity.application.security.JwtAuthenticatedUser;
import com.erp.platform.identity.application.security.JwtService;
import com.erp.platform.security.authentication.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter.
 *
 * <p>Extracts the Bearer token from the Authorization header, validates it,
 * and sets the authenticated user in the security context.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Header carrying the selected company (legal entity) id. */
    public static final String HEADER_COMPANY_ID = "X-Company-Id";

    /** Header carrying the selected fiscal year id. */
    public static final String HEADER_FISCAL_YEAR_ID = "X-Fiscal-Year-Id";

    /** Header carrying the request id. */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** Header carrying the correlation id used for tracing. */
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Tracing / client metadata is useful even for unauthenticated calls.
        RequestContext.setRequestId(request.getHeader(HEADER_REQUEST_ID));
        RequestContext.setCorrelationId(request.getHeader(HEADER_CORRELATION_ID));
        RequestContext.setClientIp(request.getRemoteAddr());
        RequestContext.setUserAgent(request.getHeader("User-Agent"));

        final String authHeader = request.getHeader("Authorization");

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    AccessTokenClaims claims = jwtService.parseAccessToken(jwt);
                    String userId = claims.userId();

                    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Set tenant context from JWT claims
                        if (claims.tenantId() != null) {
                            TenantContext.setTenantId(claims.tenantId().toString());
                        }

                        // Populate request context from validated JWT claims
                        RequestContext.setUserId(userId);
                        if (claims.tenantId() != null) {
                            RequestContext.setTenantId(claims.tenantId().toString());
                        }

                        // Company / fiscal year are request-scoped selections rather
                        // than token claims, so the user can switch them without
                        // re-authenticating. They are read only from the headers and
                        // never from the body or query string.
                        RequestContext.setCompanyId(request.getHeader(HEADER_COMPANY_ID));
                        RequestContext.setFiscalYearId(request.getHeader(HEADER_FISCAL_YEAR_ID));

                        // Create authenticated user
                        AuthenticatedUser authenticatedUser = JwtAuthenticatedUser.from(claims);

                        // Populate authorities from token claims so Spring Security method-level
                        // checks (@PreAuthorize / hasRole) work in addition to the custom aspect.
                        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                        for (String role : claims.roles()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                        for (String permission : claims.permissions()) {
                            authorities.add(new SimpleGrantedAuthority(permission));
                        }

                        // Set authentication in security context
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                authenticatedUser,
                                null,
                                authorities
                        );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (Exception e) {
                    log.debug("Failed to validate JWT token: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear thread-locals and security context so nothing leaks to
            // the next request served by this pooled thread.
            TenantContext.clear();
            RequestContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}

