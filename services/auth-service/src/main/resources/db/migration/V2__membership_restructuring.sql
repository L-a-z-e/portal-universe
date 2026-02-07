-- ===================================================================
-- V2: 역할+서비스 복합 멤버십 재구조화 (ADR-021)
-- - membership_tiers: service_name → membership_group
-- - user_memberships: service_name → membership_group
-- - roles: ROLE_SELLER → ROLE_SHOPPING_SELLER, membership_group 추가
-- - users: role enum 컬럼 삭제
-- ===================================================================

-- ---------------------------------------------------------------
-- 1. membership_tiers: service_name → membership_group
-- ---------------------------------------------------------------

-- 1a. 새 컬럼 추가
ALTER TABLE `membership_tiers` ADD COLUMN `membership_group` VARCHAR(50) NULL AFTER `id`;

-- 1b. 기존 데이터 변환: service_name → "user:{service_name}"
UPDATE `membership_tiers` SET `membership_group` = CONCAT('user:', `service_name`);

-- 1c. NOT NULL 제약조건 적용
ALTER TABLE `membership_tiers` MODIFY COLUMN `membership_group` VARCHAR(50) NOT NULL;

-- 1d. 기존 유니크 키 삭제 + 새 유니크 키 생성
ALTER TABLE `membership_tiers` DROP INDEX `uk_service_tier`;
ALTER TABLE `membership_tiers` ADD CONSTRAINT `uk_group_tier` UNIQUE (`membership_group`, `tier_key`);

-- 1e. service_name 컬럼 삭제
ALTER TABLE `membership_tiers` DROP COLUMN `service_name`;

-- 1f. 기존 shopping 티어 → user:shopping 으로 변환 완료, blog도 동일
-- 새 seller:shopping 티어 삽입
INSERT IGNORE INTO `membership_tiers` (`membership_group`, `tier_key`, `display_name`, `price_monthly`, `price_yearly`, `sort_order`, `is_active`, `created_at`) VALUES
('seller:shopping', 'BRONZE', 'Bronze', NULL, NULL, 0, 1, NOW()),
('seller:shopping', 'SILVER', 'Silver', 29000, 290000, 1, 1, NOW()),
('seller:shopping', 'GOLD', 'Gold', 59000, 590000, 2, 1, NOW()),
('seller:shopping', 'PLATINUM', 'Platinum', 99000, 990000, 3, 1, NOW());

-- 1g. 기존 user:blog 티어명 변경: BASIC→PRO, PREMIUM→MAX (VIP 삭제)
-- 먼저 VIP 티어에 연결된 user_memberships가 있으면 PREMIUM으로 이동 (아래 user_memberships 변환 후 처리)
-- user:blog 티어 정리는 user_memberships 변환 후에 수행

-- ---------------------------------------------------------------
-- 2. user_memberships: service_name → membership_group
-- ---------------------------------------------------------------

-- 2a. 새 컬럼 추가
ALTER TABLE `user_memberships` ADD COLUMN `membership_group` VARCHAR(50) NULL AFTER `user_id`;

-- 2b. 기존 데이터 변환: service_name → "user:{service_name}"
UPDATE `user_memberships` SET `membership_group` = CONCAT('user:', `service_name`);

-- 2c. NOT NULL 제약조건 적용
ALTER TABLE `user_memberships` MODIFY COLUMN `membership_group` VARCHAR(50) NOT NULL;

-- 2d. 기존 유니크 키 삭제 + 새 유니크 키 생성
ALTER TABLE `user_memberships` DROP INDEX `uk_user_service`;
ALTER TABLE `user_memberships` ADD CONSTRAINT `uk_user_group` UNIQUE (`user_id`, `membership_group`);

-- 2e. service_name 컬럼 삭제
ALTER TABLE `user_memberships` DROP COLUMN `service_name`;

-- ---------------------------------------------------------------
-- 3. user:blog 티어 재구성: FREE/BASIC/PREMIUM/VIP → FREE/PRO/MAX
-- ---------------------------------------------------------------

-- 3a. VIP 사용자 → PREMIUM(MAX)으로 이동
UPDATE `user_memberships` um
  JOIN `membership_tiers` mt_vip ON um.tier_id = mt_vip.id AND mt_vip.membership_group = 'user:blog' AND mt_vip.tier_key = 'VIP'
  JOIN `membership_tiers` mt_max ON mt_max.membership_group = 'user:blog' AND mt_max.tier_key = 'PREMIUM'
  SET um.tier_id = mt_max.id;

-- 3b. VIP 티어 삭제
DELETE FROM `membership_tier_permissions` WHERE tier_id IN (
  SELECT id FROM `membership_tiers` WHERE membership_group = 'user:blog' AND tier_key = 'VIP'
);
DELETE FROM `membership_tiers` WHERE membership_group = 'user:blog' AND tier_key = 'VIP';

-- 3c. BASIC → PRO 이름 변경
UPDATE `membership_tiers` SET `tier_key` = 'PRO', `display_name` = 'Pro'
  WHERE `membership_group` = 'user:blog' AND `tier_key` = 'BASIC';

-- 3d. PREMIUM → MAX 이름 변경, sort_order 조정
UPDATE `membership_tiers` SET `tier_key` = 'MAX', `display_name` = 'Max', `sort_order` = 2
  WHERE `membership_group` = 'user:blog' AND `tier_key` = 'PREMIUM';

-- 3e. PRO sort_order 조정
UPDATE `membership_tiers` SET `sort_order` = 1
  WHERE `membership_group` = 'user:blog' AND `tier_key` = 'PRO';

-- ---------------------------------------------------------------
-- 4. roles: ROLE_SELLER → ROLE_SHOPPING_SELLER + membership_group 추가
-- ---------------------------------------------------------------

-- 4a. membership_group 컬럼 추가 (nullable — admin 역할은 null)
ALTER TABLE `roles` ADD COLUMN `membership_group` VARCHAR(50) NULL;

-- 4b. ROLE_SELLER → ROLE_SHOPPING_SELLER
UPDATE `roles` SET `role_key` = 'ROLE_SHOPPING_SELLER',
                   `display_name` = 'Shopping Seller',
                   `description` = 'Shopping service seller who can manage own products',
                   `membership_group` = 'seller:shopping'
  WHERE `role_key` = 'ROLE_SELLER';

-- 4c. ROLE_SHOPPING_ADMIN의 parentRole을 ROLE_SHOPPING_SELLER로 변경
-- (SUPER_ADMIN → SHOPPING_ADMIN → SHOPPING_SELLER → USER 계층)
UPDATE `roles` r
  JOIN `roles` seller ON seller.role_key = 'ROLE_SHOPPING_SELLER'
  SET r.parent_role_id = seller.id
  WHERE r.role_key = 'ROLE_SHOPPING_ADMIN';

-- 4d. ROLE_BLOG_ADMIN의 parentRole을 ROLE_USER로 변경
-- (SUPER_ADMIN → BLOG_ADMIN → USER 계층)
UPDATE `roles` r
  JOIN `roles` user_role ON user_role.role_key = 'ROLE_USER'
  SET r.parent_role_id = user_role.id
  WHERE r.role_key = 'ROLE_BLOG_ADMIN';

-- ---------------------------------------------------------------
-- 5. users: role enum 컬럼 삭제
-- ---------------------------------------------------------------
ALTER TABLE `users` DROP COLUMN `role`;

-- ---------------------------------------------------------------
-- 6. 인덱스 추가
-- ---------------------------------------------------------------
CREATE INDEX `idx_mt_membership_group` ON `membership_tiers` (`membership_group`);
CREATE INDEX `idx_um_membership_group` ON `user_memberships` (`membership_group`);
