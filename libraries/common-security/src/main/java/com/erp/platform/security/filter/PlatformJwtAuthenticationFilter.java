package com.erp.platform.security.filter;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.security.token.PlatformTokenClaims;
import com.erp.platform.security.token.PlatformTokenValidator;
import com.erp.platform.security.token.TokenAuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared JWT authentication filter for all platform and business services.
 *
 * <p>Performs three jobs in one pass:
 * <ol>
 *   <li>Validates the {@code Authorization: Bearer} access token issued by the Identity
 *       Service and populates Spring Security's context from its claims.</li>
 *   <li>Populates {@link RequestContext} / {@link TenantContext} with the user and tenant
 *       taken from the <em>validated token</em> (never from client-supplied input).</li>
 *   <li>Populates the request-scoped company / fiscal-year selection from the
 *       {@code X-Company-Id} and {@code X-Fiscal-Year-Id} headers, which the API Gateway
 *       forwards. These are read <em>only</em> here, never from request bodies or query
 *       parameters, so a client cannot spoof another company's data.</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Slf4j
public class PlatformJwtAuthenticationFilter extends OncePerRequestFilter {

    /** Header carrying the selected company (legal entity) id. */
    public static final String HEADER_COMPANY_ID = "X-Company-Id";

    /** Header carrying the selected fiscal year id. */
    public static final String HEADER_FISCAL_YEAR_ID = "X-Fiscal-Year-Id";

    /** Header carrying the correlation id used for tracing. */
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    /** Header carrying the request id. */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final PlatformTokenValidator validator;

    public PlatformJwtAuthenticationFilter(PlatformTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            RequestContext.setCorrelationId(request.getHeader(HEADER_CORRELATION_ID));
            RequestContext.setRequestId(request.getHeader(HEADER_REQUEST_ID));
            RequestContext.setClientIp(request.getRemoteAddr());
            RequestContext.setUserAgent(request.getHeader("User-Agent"));

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    PlatformTokenClaims claims = validator.validate(authHeader.substring(7));

                    if (claims.tenantId() != null) {
                        TenantContext.setTenantId(claims.tenantId().toString());
                        RequestContext.setTenantId(claims.tenantId().toString());
                    }
                    if (claims.userId() != null) {
                        RequestContext.setUserId(claims.userId());
                    }

                    // Company / fiscal year are request-scoped selections, not token claims,
                    // so the user can switch them without re-authenticating.
                    RequestContext.setCompanyId(request.getHeader(HEADER_COMPANY_ID));
                    RequestContext.setFiscalYearId(request.getHeader(HEADER_FISCAL_YEAR_ID));

                    AuthenticatedUser user = TokenAuthenticatedUser.from(claims);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, toAuthorities(claims));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    // Invalid/expired token: leave the context anonymous and let the
                    // security entry point decide (normally a 401).
                    log.debug("Rejected access token: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear thread-locals to avoid leaking context between pooled threads.
            TenantContext.clear();
            RequestContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private List<SimpleGrantedAuthority> toAuthorities(PlatformTokenClaims claims) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : claims.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : claims.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return authorities;
    }
}
