-- V3: Allow null target_user_id in audit log for role/permission management operations
ALTER TABLE `auth_audit_log` MODIFY COLUMN `target_user_id` VARCHAR(255) NULL;
