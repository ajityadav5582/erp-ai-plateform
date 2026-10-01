-- Seed local_level_types data
-- Migration: V14
-- Description: Seeds 4 local level types of Nepal into the local_level_types table.
-- This data is used to categorize local bodies (municipalities, cities, etc.).

INSERT INTO local_level_types (
    local_level_type_id,
    local_level_type_code,
    local_level_type_name,
    nepali_name,
    country_code,
    status,
    created_at,
    version
) VALUES
(
    'LLT-1',
    'METRO',
    'Metropolitan City',
    'महानगरपालिका',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    'LLT-2',
    'SUB_METRO',
    'Sub-Metropolitan City',
    'उपमहानगरपालिका',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    'LLT-3',
    'MUNICIPALITY',
    'Municipality',
    'नगरपालिका',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    'LLT-4',
    'RURAL_MUNICIPALITY',
    'Rural Municipality',
    'गाउँपालिका',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (local_level_type_code) DO NOTHING;
