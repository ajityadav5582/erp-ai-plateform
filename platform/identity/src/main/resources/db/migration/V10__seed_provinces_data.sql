-- Seed provinces data
-- Migration: V10
-- Description: Seeds the 7 provinces of Nepal into the provinces table.
-- This data is used for geographic management and organizational structure.

INSERT INTO provinces (
    province_code,
    province_name,
    nepali_name,
    country_code,
    status,
    created_at,
    version
) VALUES
(
    '1',
    'Koshi Pradesh',
    'कोशी प्रदेश',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    '2',
    'Madhesh Pradesh',
    'मधेश प्रदेश',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    '3',
    'Bagmati Pradesh',
    'बागमती प्रदेश',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    '4',
    'Gandaki Pradesh',
    'गण्डकी प्रदेश',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    '5',
    'Lumbini Pradesh',
    'लुम्बिनी प्रदेश',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    '6',
    'Karnali Pradesh',
    'कर्णाली प्रदेश',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
),
(
    '7',
    'Sudurpashchim Pradesh',
    'सुदूरपश्चिम प्रदेश',
    'NPL',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (province_code) DO NOTHING;
