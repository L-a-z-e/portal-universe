-- RBAC Legacy Cleanup: Remove deprecated 'role' column from users table.
-- The RBAC system now uses the 'user_roles' table instead.
-- This migration is safe to run after RbacDataMigrationRunner has completed.
ALTER TABLE users DROP COLUMN role;
