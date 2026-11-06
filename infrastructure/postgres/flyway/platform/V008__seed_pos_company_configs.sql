-- Migration: V008
-- Description: Seed a default pos_company_configs row for every existing company
-- that does not yet have one. Defaults: all payment methods enabled (null),
-- cash rounding on, zero service charge, NONE tip handling, no defaults set.
-- Guarded so it is a no-op when the companies table does not exist yet (e.g.
-- before the identity service has created its schema).

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'companies'
    ) THEN
        INSERT INTO pos_company_configs (
            tenant_id, company_id, enabled_payment_methods, cash_rounding,
            service_charge_percent, tip_handling, default_register_id,
            default_warehouse_id, price_list_id, restaurant_settings,
            created_at, version
        )
        SELECT c.tenant_id, c.id, NULL, TRUE, 0, 'NONE', NULL, NULL, NULL, NULL,
               CURRENT_TIMESTAMP, 0
        FROM companies c
        WHERE NOT EXISTS (
            SELECT 1 FROM pos_company_configs p
            WHERE p.tenant_id = c.tenant_id AND p.company_id = c.id
        );
    END IF;
EXCEPTION WHEN others THEN
    NULL;
END $$;
