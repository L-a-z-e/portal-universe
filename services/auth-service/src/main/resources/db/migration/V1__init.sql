-- ===================================================================
-- Auth Service - Consolidated Init Schema (PostgreSQL)
-- Merged: V1__baseline + V2__membership_restructuring +
--         V3__audit_log_nullable_target + V4__role_multi_include +
--         V5__role_default_membership_mapping
-- Author: Laze
-- ===================================================================

-- -------------------------------------------------------------------
-- Trigger function: update_updated_at_column
-- -------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------------------
-- Tables
-- -------------------------------------------------------------------

-- 사용자
CREATE TABLE IF NOT EXISTS users (
    user_id  BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
    uuid     VARCHAR(255)  NOT NULL,
    email    VARCHAR(255)  NOT NULL,
    password VARCHAR(255)  DEFAULT NULL,
    status   VARCHAR(50)   NOT NULL,
    last_login_at        TIMESTAMP(6) DEFAULT NULL,
    password_changed_at  TIMESTAMP(6) DEFAULT NULL,
    created_at           TIMESTAMP(6) DEFAULT NULL,
    updated_at           TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_uuid  UNIQUE (uuid)
);

-- 사용자 프로필
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id           BIGINT       NOT NULL,
    nickname          VARCHAR(50)  NOT NULL,
    real_name         VARCHAR(50)  DEFAULT NULL,
    username          VARCHAR(20)  DEFAULT NULL,
    bio               VARCHAR(200) DEFAULT NULL,
    phone_number      VARCHAR(20)  DEFAULT NULL,
    profile_image_url VARCHAR(255) DEFAULT NULL,
    website           VARCHAR(255) DEFAULT NULL,
    marketing_agree   BOOLEAN      NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_profiles_username UNIQUE (username),
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 소셜 계정
CREATE TABLE IF NOT EXISTS social_accounts (
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id     BIGINT       NOT NULL,
    provider    VARCHAR(50)  NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(255) DEFAULT NULL,
    connected_at TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_provider_id UNIQUE (provider, provider_id),
    CONSTRAINT fk_social_account_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 비밀번호 이력
CREATE TABLE IF NOT EXISTS password_history (
    history_id    BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id       BIGINT       NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (history_id)
);

-- 역할 (V2: membership_group 추가, V4: parent_role_id 제거)
CREATE TABLE IF NOT EXISTS roles (
    id               BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
    role_key         VARCHAR(50)   NOT NULL,
    display_name     VARCHAR(100)  NOT NULL,
    description      VARCHAR(500)  DEFAULT NULL,
    service_scope    VARCHAR(50)   DEFAULT NULL,
    membership_group VARCHAR(50)   DEFAULT NULL,
    is_system        BOOLEAN       NOT NULL,
    is_active        BOOLEAN       NOT NULL,
    created_at       TIMESTAMP(6)  DEFAULT NULL,
    updated_at       TIMESTAMP(6)  DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_key UNIQUE (role_key)
);

-- 역할 포함 관계 DAG (V4: parent_role_id 대체)
CREATE TABLE IF NOT EXISTS role_includes (
    id               BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    role_id          BIGINT       NOT NULL,
    included_role_id BIGINT       NOT NULL,
    created_at       TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_include UNIQUE (role_id, included_role_id),
    CONSTRAINT fk_ri_role          FOREIGN KEY (role_id)          REFERENCES roles (id),
    CONSTRAINT fk_ri_included_role FOREIGN KEY (included_role_id) REFERENCES roles (id)
);

-- 권한
CREATE TABLE IF NOT EXISTS permissions (
    id             BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    permission_key VARCHAR(100) NOT NULL,
    service        VARCHAR(50)  NOT NULL,
    resource       VARCHAR(50)  NOT NULL,
    action         VARCHAR(50)  NOT NULL,
    description    VARCHAR(500) DEFAULT NULL,
    is_active      BOOLEAN      NOT NULL,
    created_at     TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_key UNIQUE (permission_key)
);

-- 역할-권한 매핑
CREATE TABLE IF NOT EXISTS role_permissions (
    id            BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles (id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

-- 사용자-역할 매핑
CREATE TABLE IF NOT EXISTS user_roles (
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id     VARCHAR(255) NOT NULL,
    role_id     BIGINT       NOT NULL,
    assigned_by VARCHAR(255) DEFAULT NULL,
    assigned_at TIMESTAMP(6) NOT NULL,
    expires_at  TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- 감사 로그 (V3: target_user_id nullable)
CREATE TABLE IF NOT EXISTS auth_audit_log (
    id             BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    event_type     VARCHAR(50)  NOT NULL,
    target_user_id VARCHAR(255) DEFAULT NULL,
    actor_user_id  VARCHAR(255) DEFAULT NULL,
    details        TEXT         DEFAULT NULL,
    ip_address     VARCHAR(50)  DEFAULT NULL,
    created_at     TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 멤버십 등급 (V2: service_name → membership_group)
CREATE TABLE IF NOT EXISTS membership_tiers (
    id               BIGINT         NOT NULL GENERATED ALWAYS AS IDENTITY,
    membership_group VARCHAR(50)    NOT NULL,
    tier_key         VARCHAR(50)    NOT NULL,
    display_name     VARCHAR(100)   NOT NULL,
    price_monthly    DECIMAL(10, 2) DEFAULT NULL,
    price_yearly     DECIMAL(10, 2) DEFAULT NULL,
    sort_order       INT            NOT NULL,
    is_active        BOOLEAN        NOT NULL,
    created_at       TIMESTAMP(6)   DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_tier UNIQUE (membership_group, tier_key)
);

-- 사용자 멤버십 (V2: service_name → membership_group)
CREATE TABLE IF NOT EXISTS user_memberships (
    id               BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id          VARCHAR(255) NOT NULL,
    membership_group VARCHAR(50)  NOT NULL,
    tier_id          BIGINT       NOT NULL,
    status           VARCHAR(50)  NOT NULL,
    started_at       TIMESTAMP(6) NOT NULL,
    expires_at       TIMESTAMP(6) DEFAULT NULL,
    auto_renew       BOOLEAN      NOT NULL,
    created_at       TIMESTAMP(6) DEFAULT NULL,
    updated_at       TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_group UNIQUE (user_id, membership_group),
    CONSTRAINT fk_um_tier FOREIGN KEY (tier_id) REFERENCES membership_tiers (id)
);

-- 멤버십 등급-권한 매핑
CREATE TABLE IF NOT EXISTS membership_tier_permissions (
    id            BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    tier_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tier_permission UNIQUE (tier_id, permission_id),
    CONSTRAINT fk_mtp_tier       FOREIGN KEY (tier_id)       REFERENCES membership_tiers (id),
    CONSTRAINT fk_mtp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

-- 역할 기본 멤버십 매핑 (V5)
CREATE TABLE IF NOT EXISTS role_default_memberships (
    id               BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    role_key         VARCHAR(50) NOT NULL,
    membership_group VARCHAR(50) NOT NULL,
    default_tier_key VARCHAR(50) NOT NULL,
    created_at       TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_group UNIQUE (role_key, membership_group),
    CONSTRAINT fk_rdm_membership_tier
        FOREIGN KEY (membership_group, default_tier_key)
        REFERENCES membership_tiers (membership_group, tier_key)
);

-- 판매자 신청
CREATE TABLE IF NOT EXISTS seller_applications (
    id              BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id         VARCHAR(255) NOT NULL,
    business_name   VARCHAR(200) NOT NULL,
    business_number VARCHAR(50)  DEFAULT NULL,
    reason          TEXT         DEFAULT NULL,
    status          VARCHAR(50)  NOT NULL,
    reviewed_by     VARCHAR(255) DEFAULT NULL,
    review_comment  TEXT         DEFAULT NULL,
    reviewed_at     TIMESTAMP(6) DEFAULT NULL,
    created_at      TIMESTAMP(6) DEFAULT NULL,
    updated_at      TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 팔로우
CREATE TABLE IF NOT EXISTS follows (
    follow_id    BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    follower_id  BIGINT       NOT NULL,
    following_id BIGINT       NOT NULL,
    created_at   TIMESTAMP(6) DEFAULT NULL,
    PRIMARY KEY (follow_id),
    CONSTRAINT uk_follow_relationship UNIQUE (follower_id, following_id),
    CONSTRAINT fk_follow_follower  FOREIGN KEY (follower_id)  REFERENCES users (user_id),
    CONSTRAINT fk_follow_following FOREIGN KEY (following_id) REFERENCES users (user_id)
);

-- -------------------------------------------------------------------
-- Indexes
-- -------------------------------------------------------------------

CREATE INDEX idx_social_accounts_user_id     ON social_accounts (user_id);
CREATE INDEX idx_password_history_user_id_at ON password_history (user_id, created_at);
CREATE INDEX idx_auth_audit_log_target_user  ON auth_audit_log (target_user_id);
CREATE INDEX idx_auth_audit_log_event_type   ON auth_audit_log (event_type);
CREATE INDEX idx_auth_audit_log_created_at   ON auth_audit_log (created_at);
CREATE INDEX idx_mt_membership_group         ON membership_tiers (membership_group);
CREATE INDEX idx_um_membership_group         ON user_memberships (membership_group);
CREATE INDEX idx_follows_follower_id         ON follows (follower_id);
CREATE INDEX idx_follows_following_id        ON follows (following_id);

-- -------------------------------------------------------------------
-- Triggers (updated_at auto-update)
-- -------------------------------------------------------------------

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_user_memberships_updated_at
    BEFORE UPDATE ON user_memberships
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_seller_applications_updated_at
    BEFORE UPDATE ON seller_applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- -------------------------------------------------------------------
-- Seed Data: Membership Tiers
-- (RbacDataMigrationRunner.seedMembershipTiers() 기준 최종 상태)
-- -------------------------------------------------------------------

INSERT INTO membership_tiers (membership_group, tier_key, display_name, price_monthly, price_yearly, sort_order, is_active, created_at)
VALUES
    -- user:shopping
    ('user:shopping', 'FREE',    'Free',    NULL,    NULL,    0, TRUE, NOW()),
    ('user:shopping', 'BASIC',   'Basic',   4900,   49000,   1, TRUE, NOW()),
    ('user:shopping', 'PREMIUM', 'Premium', 9900,   99000,   2, TRUE, NOW()),
    ('user:shopping', 'VIP',     'VIP',     19900, 199000,   3, TRUE, NOW()),
    -- user:blog (V2: BASIC→PRO, PREMIUM→MAX, VIP 제거)
    ('user:blog', 'FREE', 'Free', NULL,  NULL,  0, TRUE, NOW()),
    ('user:blog', 'PRO',  'Pro',  2900, 29000,  1, TRUE, NOW()),
    ('user:blog', 'MAX',  'Max',  5900, 59000,  2, TRUE, NOW()),
    -- seller:shopping (V2)
    ('seller:shopping', 'BRONZE',   'Bronze',   NULL,    NULL,    0, TRUE, NOW()),
    ('seller:shopping', 'SILVER',   'Silver',   29000,  290000,  1, TRUE, NOW()),
    ('seller:shopping', 'GOLD',     'Gold',     59000,  590000,  2, TRUE, NOW()),
    ('seller:shopping', 'PLATINUM', 'Platinum', 99000,  990000,  3, TRUE, NOW())
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------------
-- Seed Data: Roles
-- (RbacDataMigrationRunner.seedRoles() 기준 최종 상태 — V2, V4 반영)
-- -------------------------------------------------------------------

INSERT INTO roles (role_key, display_name, description, service_scope, membership_group, is_system, is_active, created_at, updated_at)
VALUES
    ('ROLE_GUEST',          'Guest',          'Unauthenticated guest role with minimal access',          NULL,       NULL,             TRUE, TRUE, NOW(), NOW()),
    ('ROLE_USER',           'User',           'Default user role with basic access',                    NULL,       NULL,             TRUE, TRUE, NOW(), NOW()),
    ('ROLE_SUPER_ADMIN',    'Super Admin',    'System-wide administrator with full access',              NULL,       NULL,             TRUE, TRUE, NOW(), NOW()),
    ('ROLE_SHOPPING_SELLER','Shopping Seller','Shopping service seller who can manage own products',     'shopping', 'seller:shopping',TRUE, TRUE, NOW(), NOW()),
    ('ROLE_SHOPPING_ADMIN', 'Shopping Admin', 'Shopping service administrator',                         'shopping', NULL,             TRUE, TRUE, NOW(), NOW()),
    ('ROLE_BLOG_ADMIN',     'Blog Admin',     'Blog service administrator',                             'blog',     NULL,             TRUE, TRUE, NOW(), NOW())
ON CONFLICT (role_key) DO NOTHING;

-- -------------------------------------------------------------------
-- Seed Data: Role Includes DAG
-- (V4 + RbacDataMigrationRunner.seedRoles() 기준 최종 상태)
-- -------------------------------------------------------------------

-- ROLE_USER → ROLE_GUEST
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT r.id, g.id, NOW()
FROM roles r, roles g
WHERE r.role_key = 'ROLE_USER' AND g.role_key = 'ROLE_GUEST'
ON CONFLICT DO NOTHING;

-- ROLE_SHOPPING_SELLER → ROLE_USER
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT r.id, u.id, NOW()
FROM roles r, roles u
WHERE r.role_key = 'ROLE_SHOPPING_SELLER' AND u.role_key = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- ROLE_SHOPPING_ADMIN → ROLE_SHOPPING_SELLER
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT r.id, s.id, NOW()
FROM roles r, roles s
WHERE r.role_key = 'ROLE_SHOPPING_ADMIN' AND s.role_key = 'ROLE_SHOPPING_SELLER'
ON CONFLICT DO NOTHING;

-- ROLE_BLOG_ADMIN → ROLE_USER
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT r.id, u.id, NOW()
FROM roles r, roles u
WHERE r.role_key = 'ROLE_BLOG_ADMIN' AND u.role_key = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- ROLE_SUPER_ADMIN → ROLE_SHOPPING_ADMIN
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT s.id, a.id, NOW()
FROM roles s, roles a
WHERE s.role_key = 'ROLE_SUPER_ADMIN' AND a.role_key = 'ROLE_SHOPPING_ADMIN'
ON CONFLICT DO NOTHING;

-- ROLE_SUPER_ADMIN → ROLE_BLOG_ADMIN
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT s.id, a.id, NOW()
FROM roles s, roles a
WHERE s.role_key = 'ROLE_SUPER_ADMIN' AND a.role_key = 'ROLE_BLOG_ADMIN'
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------------
-- Seed Data: Permissions
-- (RbacDataMigrationRunner.seedPermissions() 기준)
-- -------------------------------------------------------------------

INSERT INTO permissions (permission_key, service, resource, action, description, is_active, created_at)
VALUES
    -- shopping permissions
    ('shopping:product:create',      'shopping', 'product',   'create',       'Create products',            TRUE, NOW()),
    ('shopping:product:read',        'shopping', 'product',   'read',         'View products',              TRUE, NOW()),
    ('shopping:product:update',      'shopping', 'product',   'update',       'Update products',            TRUE, NOW()),
    ('shopping:product:delete',      'shopping', 'product',   'delete',       'Delete products',            TRUE, NOW()),
    ('shopping:order:read',          'shopping', 'order',     'read',         'View orders',                TRUE, NOW()),
    ('shopping:order:manage',        'shopping', 'order',     'manage',       'Manage all orders',          TRUE, NOW()),
    ('shopping:inventory:manage',    'shopping', 'inventory', 'manage',       'Manage inventory',           TRUE, NOW()),
    ('shopping:delivery:manage',     'shopping', 'delivery',  'manage',       'Manage deliveries',          TRUE, NOW()),
    ('shopping:coupon:manage',       'shopping', 'coupon',    'manage',       'Manage coupons',             TRUE, NOW()),
    ('shopping:timedeal:manage',     'shopping', 'timedeal',  'manage',       'Manage time deals',          TRUE, NOW()),
    ('shopping:timedeal:early_access','shopping','timedeal',  'early_access', 'Early access to time deals', TRUE, NOW()),
    ('shopping:analytics:read',      'shopping', 'analytics', 'read',         'View shopping analytics',    TRUE, NOW()),
    -- blog permissions
    ('blog:post:create',     'blog', 'post',     'create',  'Create blog posts',     TRUE, NOW()),
    ('blog:post:read',       'blog', 'post',     'read',    'Read blog posts',       TRUE, NOW()),
    ('blog:post:update',     'blog', 'post',     'update',  'Update blog posts',     TRUE, NOW()),
    ('blog:post:delete',     'blog', 'post',     'delete',  'Delete blog posts',     TRUE, NOW()),
    ('blog:post:manage',     'blog', 'post',     'manage',  'Manage all blog posts', TRUE, NOW()),
    ('blog:comment:manage',  'blog', 'comment',  'manage',  'Manage all comments',  TRUE, NOW()),
    ('blog:file:delete',     'blog', 'file',     'delete',  'Delete uploaded files', TRUE, NOW()),
    ('blog:analytics:read',  'blog', 'analytics','read',    'View blog analytics',   TRUE, NOW()),
    -- auth permissions
    ('auth:role:manage',       'auth', 'role',       'manage',  'Manage roles',             TRUE, NOW()),
    ('auth:permission:manage', 'auth', 'permission', 'manage',  'Manage permissions',       TRUE, NOW()),
    ('auth:membership:manage', 'auth', 'membership', 'manage',  'Manage memberships',       TRUE, NOW()),
    ('auth:user:manage',       'auth', 'user',       'manage',  'Manage users',             TRUE, NOW()),
    ('auth:seller:approve',    'auth', 'seller',     'approve', 'Approve seller applications', TRUE, NOW()),
    ('auth:audit:read',        'auth', 'audit',      'read',    'Read audit logs',          TRUE, NOW())
ON CONFLICT (permission_key) DO NOTHING;

-- -------------------------------------------------------------------
-- Seed Data: Role-Permission Mappings
-- (RbacDataMigrationRunner.mapRolePermissions() 기준)
-- -------------------------------------------------------------------

-- ROLE_USER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_key = 'ROLE_USER'
  AND p.permission_key IN (
      'shopping:product:read',
      'shopping:order:read',
      'blog:post:create',
      'blog:post:read',
      'blog:post:update',
      'blog:post:delete'
  )
ON CONFLICT DO NOTHING;

-- ROLE_SHOPPING_SELLER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_key = 'ROLE_SHOPPING_SELLER'
  AND p.permission_key IN (
      'shopping:product:create',
      'shopping:product:read',
      'shopping:product:update',
      'shopping:product:delete',
      'shopping:order:read',
      'shopping:analytics:read'
  )
ON CONFLICT DO NOTHING;

-- ROLE_SHOPPING_ADMIN permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_key = 'ROLE_SHOPPING_ADMIN'
  AND p.permission_key IN (
      'shopping:product:create',
      'shopping:product:read',
      'shopping:product:update',
      'shopping:product:delete',
      'shopping:order:read',
      'shopping:order:manage',
      'shopping:inventory:manage',
      'shopping:delivery:manage',
      'shopping:coupon:manage',
      'shopping:timedeal:manage',
      'shopping:analytics:read',
      'auth:seller:approve'
  )
ON CONFLICT DO NOTHING;

-- ROLE_BLOG_ADMIN permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_key = 'ROLE_BLOG_ADMIN'
  AND p.permission_key IN (
      'blog:post:create',
      'blog:post:read',
      'blog:post:update',
      'blog:post:delete',
      'blog:post:manage',
      'blog:comment:manage',
      'blog:file:delete',
      'blog:analytics:read'
  )
ON CONFLICT DO NOTHING;

-- ROLE_SUPER_ADMIN permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_key = 'ROLE_SUPER_ADMIN'
  AND p.permission_key IN (
      'auth:role:manage',
      'auth:permission:manage',
      'auth:membership:manage',
      'auth:user:manage',
      'auth:seller:approve',
      'auth:audit:read'
  )
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------------
-- Seed Data: Role Default Memberships (V5)
-- -------------------------------------------------------------------

INSERT INTO role_default_memberships (role_key, membership_group, default_tier_key, created_at)
VALUES
    ('ROLE_USER',            'user:blog',      'FREE',   NOW()),
    ('ROLE_USER',            'user:shopping',  'FREE',   NOW()),
    ('ROLE_SHOPPING_SELLER', 'seller:shopping','BRONZE', NOW())
ON CONFLICT DO NOTHING;
