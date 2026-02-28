-- ===================================================================
-- Shopping Service - Add missing BaseEntity columns
-- delivery_histories was missing updated_at
-- ===================================================================

ALTER TABLE delivery_histories ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
