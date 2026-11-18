-- Migration: V038
-- Description: Track completion of database bootstrap/master-data seeding steps.
-- Used by the application bootstrap runner to make seeding idempotent and observable.

CREATE TABLE IF NOT EXISTS db_bootstrap_state (
    id BIGSERIAL PRIMARY KEY,
    step_name VARCHAR(128) NOT NULL,
    step_version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    details TEXT,
    CONSTRAINT uk_db_bootstrap_step UNIQUE (step_name, step_version)
);

CREATE INDEX IF NOT EXISTS idx_db_bootstrap_state_step ON db_bootstrap_state (step_name);
CREATE INDEX IF NOT EXISTS idx_db_bootstrap_state_status ON db_bootstrap_state (status);
