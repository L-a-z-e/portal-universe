-- ===================================================================
-- Shopping Service - Add remaining missing BaseEntity columns
-- product_images and stock_movements need updated_at for BaseEntity
-- ===================================================================

ALTER TABLE product_images ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
