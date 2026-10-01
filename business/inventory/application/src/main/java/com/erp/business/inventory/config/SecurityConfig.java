package com.erp.business.inventory.config;

import com.erp.platform.security.filter.PlatformJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Inventory Service.
 *
 * <p>The service never issues tokens; it only validates the access token minted
 * by the Identity Service using the shared
 * {@link com.erp.platform.security.filter.PlatformJwtAuthenticationFilter} from
 * {@code libraries:common-security}. That filter also publishes the authenticated
 * tenant/user and the selected company/fiscal year on
 * {@link com.erp.platform.common.context.RequestContext}.
 *
 * <p>CORS is disabled here on purpose: the API Gateway on port 8080 is the single
 * CORS authority, and allowing downstream services to emit CORS headers produced
 * duplicate {@code Access-Control-Allow-Origin} values during preflight.
 *
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final PlatformJwtAuthenticationFilter platformJwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/inventory/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(platformJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
