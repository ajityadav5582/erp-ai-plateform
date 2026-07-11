package com.erp.platform.identity.application.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the BCrypt {@link PasswordEncoder} used to hash and verify user passwords.
 *
 * <p>Only the crypto portion of Spring Security is used (no filter chain), keeping the
 * authentication subsystem self-contained while following platform password-hashing
 * standards (BCrypt with a configurable work factor).
 *
 * @since 1.0.0
 */
@Configuration
public class PasswordEncoderConfig {

    /** BCrypt work factor (cost). 12 is a strong, production-suitable default. */
    private static final int BCRYPT_STRENGTH = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
