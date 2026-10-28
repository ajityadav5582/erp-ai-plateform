-- Create token tables for the authentication subsystem
-- Migration: V7
-- Description: Creates password_reset_tokens and refresh_tokens tables that back
-- the PasswordResetToken and RefreshToken JPA entities. These were missing from
-- earlier migrations, causing schema-validation to fail on startup.

-- =============================================================================
-- password_reset_tokens
-- =============================================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

-- Unique constraint on token_id (matches @UniqueConstraint in entity)
CREATE UNIQUE INDEX IF NOT EXISTS uk_password_reset_tokens_token_id
    ON password_reset_tokens(token_id);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_tenant_id
    ON password_reset_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token_hash
    ON password_reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at
    ON password_reset_tokens(expires_at);

-- =============================================================================
-- refresh_tokens
-- =============================================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL
);

-- Unique constraint on token_id (matches @UniqueConstraint in entity)
CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_tokens_token_id
    ON refresh_tokens(token_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_tenant_id
    ON refresh_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash
    ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);
