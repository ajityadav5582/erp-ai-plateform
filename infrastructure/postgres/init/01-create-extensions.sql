-- =============================================================================
-- ERP AI Platform - PostgreSQL Extensions
-- Creates common extensions for all databases
-- =============================================================================
-- This script enables extensions that are useful across all microservices.
-- =============================================================================

-- UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Full-text search
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- JSONB operators
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- Range types
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- Statistical functions
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Page checksums
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
