package com.erp.platform.security.autoconfigure;

import com.erp.platform.security.context.DefaultSecurityContext;
import com.erp.platform.security.context.SecurityContext;
import com.erp.platform.security.filter.PlatformJwtAuthenticationFilter;
import com.erp.platform.security.token.PlatformJwtProperties;
import com.erp.platform.security.token.PlatformTokenValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that gives every service depending on {@code common-security}
 * a working authenticated request context.
 *
 * <p>Importing this module is enough to get:
 * <ul>
 *   <li>access-token validation and Spring Security context population,</li>
 *   <li>tenant/user context on {@link com.erp.platform.common.context.RequestContext},</li>
 *   <li>company / fiscal-year selection from request headers.</li>
 * </ul>
 *
 * <p>Services still declare their own {@code SecurityFilterChain} to decide which
 * paths require authentication; this configuration only supplies the filter and the
 * context beans.
 *
 * <p>Set {@code erp.security.jwt.enabled=false} to opt out (for example in the
 * Identity Service, which issues tokens and installs its own filter).
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(PlatformJwtProperties.class)
@ConditionalOnProperty(prefix = "erp.security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PlatformTokenValidator platformTokenValidator(PlatformJwtProperties properties) {
        return new PlatformTokenValidator(properties.getSecret(), properties.getIssuer());
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformJwtAuthenticationFilter platformJwtAuthenticationFilter(PlatformTokenValidator validator) {
        return new PlatformJwtAuthenticationFilter(validator);
    }

    /**
     * Registers the filter at highest precedence so it runs before authorization.
     */
    @Bean
    public FilterRegistrationBean<PlatformJwtAuthenticationFilter> platformJwtFilterRegistration(
            PlatformJwtAuthenticationFilter filter) {
        FilterRegistrationBean<PlatformJwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityContext.class)
    public SecurityContext platformSecurityContext() {
        return new DefaultSecurityContext();
    }
}
