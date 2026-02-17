-- V5: Role-Default Membership Mapping (ADR-045)
-- 역할 할당 시 자동으로 부여할 기본 멤버십 매핑 테이블

CREATE TABLE role_default_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_key VARCHAR(50) NOT NULL,
    membership_group VARCHAR(50) NOT NULL,
    default_tier_key VARCHAR(50) NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_group (role_key, membership_group),
    CONSTRAINT fk_rdm_membership_tier
        FOREIGN KEY (membership_group, default_tier_key)
        REFERENCES membership_tiers (membership_group, tier_key)
);

-- 기존 하드코딩 이관: ROLE_USER → user:blog/FREE, user:shopping/FREE
INSERT INTO role_default_memberships (role_key, membership_group, default_tier_key, created_at)
VALUES
    ('ROLE_USER', 'user:blog', 'FREE', NOW()),
    ('ROLE_USER', 'user:shopping', 'FREE', NOW()),
    ('ROLE_SHOPPING_SELLER', 'seller:shopping', 'BRONZE', NOW());
