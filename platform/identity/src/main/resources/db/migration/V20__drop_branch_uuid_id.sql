-- Drop the legacy UUID business identifier column from the branches table.
-- Migration: V20
-- Description:
--   The branch aggregate no longer exposes a UUID business identifier
--   (branch_id). Branches are now referenced solely by their numeric primary
--   key (id). This migration removes the leftover branch_id UUID column and
--   its associated index/constraint so the schema matches the entity model.

-- Drop the unique index that referenced the removed column.
DROP INDEX IF EXISTS idx_branches_branch_id;

-- Drop the unique constraint / column itself (idempotent).
ALTER TABLE branches DROP COLUMN IF EXISTS branch_id;
