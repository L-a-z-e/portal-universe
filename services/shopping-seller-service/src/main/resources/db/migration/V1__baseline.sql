-- ===================================================================
-- Shopping Seller Service - Baseline Schema
-- ===================================================================

-- 판매자
CREATE TABLE IF NOT EXISTS `sellers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `business_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `business_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `representative_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `commission_rate` decimal(5,2) NOT NULL DEFAULT 10.00,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sellers_user_id` (`user_id`),
  KEY `idx_sellers_status` (`status`),
  KEY `idx_sellers_business_name` (`business_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품
CREATE TABLE IF NOT EXISTS `products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seller_id` bigint NOT NULL DEFAULT 1,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `price` decimal(19,2) NOT NULL,
  `stock` int NOT NULL DEFAULT 0,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_products_name` (`name`),
  KEY `idx_products_seller_id` (`seller_id`),
  KEY `idx_products_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 재고
CREATE TABLE IF NOT EXISTS `inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `available_quantity` int NOT NULL DEFAULT 0,
  `reserved_quantity` int NOT NULL DEFAULT 0,
  `total_quantity` int NOT NULL DEFAULT 0,
  `version` bigint DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_inventory_product_id` (`product_id`),
  CONSTRAINT `fk_inventory_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 재고 이동 이력
CREATE TABLE IF NOT EXISTS `stock_movements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inventory_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `movement_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` int NOT NULL,
  `previous_available` int NOT NULL,
  `after_available` int NOT NULL,
  `previous_reserved` int NOT NULL,
  `after_reserved` int NOT NULL,
  `reference_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reference_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `performed_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stock_movement_inventory_id` (`inventory_id`),
  KEY `idx_stock_movement_product_id` (`product_id`),
  KEY `idx_stock_movement_reference` (`reference_type`,`reference_id`),
  KEY `idx_stock_movement_created_at` (`created_at`),
  CONSTRAINT `fk_stock_movement_inventory` FOREIGN KEY (`inventory_id`) REFERENCES `inventory` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 쿠폰
CREATE TABLE IF NOT EXISTS `coupons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seller_id` bigint NOT NULL DEFAULT 1,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `discount_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_value` decimal(10,2) NOT NULL,
  `minimum_order_amount` decimal(10,2) DEFAULT NULL,
  `maximum_discount_amount` decimal(10,2) DEFAULT NULL,
  `total_quantity` int NOT NULL,
  `issued_quantity` int NOT NULL DEFAULT 0,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `starts_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupons_code` (`code`),
  KEY `idx_coupons_seller_id` (`seller_id`),
  KEY `idx_coupons_status` (`status`),
  KEY `idx_coupons_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 타임딜
CREATE TABLE IF NOT EXISTS `time_deals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seller_id` bigint NOT NULL DEFAULT 1,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCHEDULED',
  `starts_at` datetime NOT NULL,
  `ends_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_time_deals_seller_id` (`seller_id`),
  KEY `idx_time_deals_status` (`status`),
  KEY `idx_time_deals_starts_at` (`starts_at`),
  KEY `idx_time_deals_ends_at` (`ends_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 타임딜 상품
CREATE TABLE IF NOT EXISTS `time_deal_products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `time_deal_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `deal_price` decimal(10,2) NOT NULL,
  `deal_quantity` int NOT NULL,
  `sold_quantity` int NOT NULL DEFAULT 0,
  `max_per_user` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_tdp_time_deal_id` (`time_deal_id`),
  KEY `idx_tdp_product_id` (`product_id`),
  CONSTRAINT `fk_tdp_time_deal` FOREIGN KEY (`time_deal_id`) REFERENCES `time_deals` (`id`),
  CONSTRAINT `fk_tdp_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 대기열
CREATE TABLE IF NOT EXISTS `waiting_queues` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_id` bigint NOT NULL,
  `max_capacity` int NOT NULL,
  `entry_batch_size` int NOT NULL,
  `entry_interval_seconds` int NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `activated_at` datetime DEFAULT NULL,
  `deactivated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_waiting_queues_event` (`event_type`,`event_id`),
  KEY `idx_waiting_queues_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 대기열 항목
CREATE TABLE IF NOT EXISTS `queue_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `queue_id` bigint NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entry_token` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WAITING',
  `joined_at` datetime NOT NULL,
  `entered_at` datetime DEFAULT NULL,
  `expired_at` datetime DEFAULT NULL,
  `left_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_token` (`entry_token`),
  KEY `idx_queue_entry_queue_user` (`queue_id`,`user_id`),
  KEY `idx_queue_entry_status` (`status`),
  CONSTRAINT `fk_queue_entry_queue` FOREIGN KEY (`queue_id`) REFERENCES `waiting_queues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
