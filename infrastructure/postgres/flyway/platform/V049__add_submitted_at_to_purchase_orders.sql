-- V049__add_submitted_at_to_purchase_orders.sql
-- Add missing submitted_at column to purchase_orders table to align with PurchaseOrder JPA entity.

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP WITH TIME ZONE;

