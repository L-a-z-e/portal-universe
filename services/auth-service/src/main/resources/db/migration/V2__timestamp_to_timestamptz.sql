-- ===================================================================
-- Auth Service - Migrate TIMESTAMP(6) → TIMESTAMPTZ(6)
-- Project-wide Instant migration (LocalDateTime → Instant)
-- Author: Laze
-- ===================================================================

-- -------------------------------------------------------------------
-- 1. Convert existing TIMESTAMP(6) columns to TIMESTAMPTZ(6)
--    USING ... AT TIME ZONE 'Asia/Seoul' interprets existing values
--    as Asia/Seoul local time and converts them to UTC-based TIMESTAMPTZ.
-- -------------------------------------------------------------------

-- users
ALTER TABLE users ALTER COLUMN last_login_at       TYPE TIMESTAMPTZ(6) USING last_login_at       AT TIME ZONE 'Asia/Seoul';
ALTER TABLE users ALTER COLUMN password_changed_at  TYPE TIMESTAMPTZ(6) USING password_changed_at  AT TIME ZONE 'Asia/Seoul';
ALTER TABLE users ALTER COLUMN created_at           TYPE TIMESTAMPTZ(6) USING created_at           AT TIME ZONE 'Asia/Seoul';
ALTER TABLE users ALTER COLUMN updated_at           TYPE TIMESTAMPTZ(6) USING updated_at           AT TIME ZONE 'Asia/Seoul';

-- social_accounts
ALTER TABLE social_accounts ALTER COLUMN connected_at TYPE TIMESTAMPTZ(6) USING connected_at AT TIME ZONE 'Asia/Seoul';

-- password_history
ALTER TABLE password_history ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';

-- roles
ALTER TABLE roles ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE roles ALTER COLUMN updated_at TYPE TIMESTAMPTZ(6) USING updated_at AT TIME ZONE 'Asia/Seoul';

-- role_includes
ALTER TABLE role_includes ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';

-- permissions
ALTER TABLE permissions ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';

-- user_roles
ALTER TABLE user_roles ALTER COLUMN assigned_at TYPE TIMESTAMPTZ(6) USING assigned_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE user_roles ALTER COLUMN expires_at  TYPE TIMESTAMPTZ(6) USING expires_at  AT TIME ZONE 'Asia/Seoul';

-- auth_audit_log
ALTER TABLE auth_audit_log ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';

-- membership_tiers
ALTER TABLE membership_tiers ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';

-- user_memberships
ALTER TABLE user_memberships ALTER COLUMN started_at  TYPE TIMESTAMPTZ(6) USING started_at  AT TIME ZONE 'Asia/Seoul';
ALTER TABLE user_memberships ALTER COLUMN expires_at  TYPE TIMESTAMPTZ(6) USING expires_at  AT TIME ZONE 'Asia/Seoul';
ALTER TABLE user_memberships ALTER COLUMN created_at  TYPE TIMESTAMPTZ(6) USING created_at  AT TIME ZONE 'Asia/Seoul';
ALTER TABLE user_memberships ALTER COLUMN updated_at  TYPE TIMESTAMPTZ(6) USING updated_at  AT TIME ZONE 'Asia/Seoul';

-- role_default_memberships
ALTER TABLE role_default_memberships ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';

-- seller_applications
ALTER TABLE seller_applications ALTER COLUMN reviewed_at TYPE TIMESTAMPTZ(6) USING reviewed_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE seller_applications ALTER COLUMN created_at  TYPE TIMESTAMPTZ(6) USING created_at  AT TIME ZONE 'Asia/Seoul';
ALTER TABLE seller_applications ALTER COLUMN updated_at  TYPE TIMESTAMPTZ(6) USING updated_at  AT TIME ZONE 'Asia/Seoul';

-- follows
ALTER TABLE follows ALTER COLUMN created_at TYPE TIMESTAMPTZ(6) USING created_at AT TIME ZONE 'Asia/Seoul';

-- -------------------------------------------------------------------
-- 2. Add updated_at column to tables that will use BaseEntity
--    but currently lack updated_at.
-- -------------------------------------------------------------------

ALTER TABLE social_accounts       ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;
ALTER TABLE password_history      ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;
ALTER TABLE role_includes         ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;
ALTER TABLE permissions           ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;
ALTER TABLE auth_audit_log        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;
ALTER TABLE membership_tiers      ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;
ALTER TABLE role_default_memberships ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;
ALTER TABLE follows               ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ(6) DEFAULT NULL;

-- -------------------------------------------------------------------
-- 3. Update trigger function to use TIMESTAMPTZ
-- -------------------------------------------------------------------

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
