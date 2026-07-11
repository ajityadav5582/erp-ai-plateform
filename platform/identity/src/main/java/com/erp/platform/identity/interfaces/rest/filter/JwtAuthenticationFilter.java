package com.erp.platform.identity.interfaces.rest.filter;

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

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            AccessTokenClaims claims = jwtService.parseAccessToken(jwt);
            userId = claims.userId();

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Set tenant context from JWT claims
                if (claims.tenantId() != null) {
                    TenantContext.setTenantId(claims.tenantId().toString());
                }

                // Create authenticated user
                AuthenticatedUser authenticatedUser = JwtAuthenticatedUser.from(claims);

                // Set authentication in security context
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        List.of() // authorities can be populated from roles if needed
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

        filterChain.doFilter(request, response);
    }
}
