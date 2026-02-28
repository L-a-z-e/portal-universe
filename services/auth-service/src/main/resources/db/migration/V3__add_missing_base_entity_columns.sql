-- ===================================================================
-- Auth Service - Add missing BaseEntity columns
-- social_accounts was missing created_at (had connected_at instead)
-- ===================================================================

ALTER TABLE social_accounts ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ(6) DEFAULT NOW();
