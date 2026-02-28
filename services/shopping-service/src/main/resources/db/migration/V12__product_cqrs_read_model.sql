-- Product CQRS: shopping-service products table is now a read model synced from seller-service.
-- Remove IDENTITY to allow explicit ID assignment from seller-service events.
ALTER TABLE products ALTER COLUMN id DROP IDENTITY IF EXISTS;
