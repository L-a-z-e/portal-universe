-- V4: Role Multi-Include DAG (ADR-044)
-- parentRole 단일 FK → role_includes 다대다 테이블로 전환

-- 1. role_includes 테이블 생성
CREATE TABLE role_includes (
  id bigint NOT NULL AUTO_INCREMENT,
  role_id bigint NOT NULL,
  included_role_id bigint NOT NULL,
  created_at datetime(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_include (role_id, included_role_id),
  CONSTRAINT fk_ri_role FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT fk_ri_included_role FOREIGN KEY (included_role_id) REFERENCES roles(id)
);

-- 2. ROLE_GUEST 생성
INSERT INTO roles (role_key, display_name, description, is_system, is_active, created_at, updated_at)
VALUES ('ROLE_GUEST', 'Guest', 'Unauthenticated guest role with minimal access', 1, 1, NOW(), NOW());

-- 3. 기존 parentRole → role_includes 마이그레이션
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT child.id, parent.id, NOW()
FROM roles child JOIN roles parent ON parent.id = child.parent_role_id
WHERE child.parent_role_id IS NOT NULL;

-- 4. 신규 include 관계: ROLE_USER → ROLE_GUEST
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT r.id, g.id, NOW() FROM roles r, roles g
WHERE r.role_key = 'ROLE_USER' AND g.role_key = 'ROLE_GUEST';

-- 5. 신규 include 관계: ROLE_SUPER_ADMIN → ROLE_SHOPPING_ADMIN
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT s.id, a.id, NOW() FROM roles s, roles a
WHERE s.role_key = 'ROLE_SUPER_ADMIN' AND a.role_key = 'ROLE_SHOPPING_ADMIN';

-- 6. 신규 include 관계: ROLE_SUPER_ADMIN → ROLE_BLOG_ADMIN
INSERT INTO role_includes (role_id, included_role_id, created_at)
SELECT s.id, a.id, NOW() FROM roles s, roles a
WHERE s.role_key = 'ROLE_SUPER_ADMIN' AND a.role_key = 'ROLE_BLOG_ADMIN';

-- 7. parent_role_id FK + 컬럼 제거
ALTER TABLE roles DROP FOREIGN KEY fk_role_parent;
ALTER TABLE roles DROP COLUMN parent_role_id;
