-- ===================================================================
-- Shopping Settlement Service - Baseline Schema
-- ===================================================================

-- 정산 주기
CREATE TABLE IF NOT EXISTS `settlement_periods` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `period_type` varchar(20) NOT NULL,
    `start_date` date NOT NULL,
    `end_date` date NOT NULL,
    `status` varchar(20) NOT NULL DEFAULT 'PENDING',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_settlement_periods_type` (`period_type`),
    KEY `idx_settlement_periods_status` (`status`),
    KEY `idx_settlement_periods_dates` (`start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 판매자별 정산
CREATE TABLE IF NOT EXISTS `settlements` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `period_id` bigint NOT NULL,
    `seller_id` bigint NOT NULL,
    `total_sales` decimal(15,2) NOT NULL DEFAULT 0,
    `total_orders` int NOT NULL DEFAULT 0,
    `total_refunds` decimal(15,2) NOT NULL DEFAULT 0,
    `commission_amount` decimal(15,2) NOT NULL DEFAULT 0,
    `net_amount` decimal(15,2) NOT NULL DEFAULT 0,
    `status` varchar(20) NOT NULL DEFAULT 'CALCULATED',
    `paid_at` datetime DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlements_period_seller` (`period_id`, `seller_id`),
    KEY `idx_settlements_seller_id` (`seller_id`),
    KEY `idx_settlements_status` (`status`),
    CONSTRAINT `fk_settlements_period` FOREIGN KEY (`period_id`) REFERENCES `settlement_periods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 정산 상세 (주문 단위)
CREATE TABLE IF NOT EXISTS `settlement_details` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `settlement_id` bigint NOT NULL,
    `order_number` varchar(50) NOT NULL,
    `order_amount` decimal(15,2) NOT NULL,
    `refund_amount` decimal(15,2) NOT NULL DEFAULT 0,
    `commission_rate` decimal(5,2) NOT NULL,
    `commission_amount` decimal(15,2) NOT NULL,
    `net_amount` decimal(15,2) NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_settlement_details_settlement` (`settlement_id`),
    KEY `idx_settlement_details_order` (`order_number`),
    CONSTRAINT `fk_settlement_details_settlement` FOREIGN KEY (`settlement_id`) REFERENCES `settlements` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 정산 원장 (Kafka 이벤트에서 수집)
CREATE TABLE IF NOT EXISTS `settlement_ledger` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_number` varchar(50) NOT NULL,
    `seller_id` bigint NOT NULL,
    `event_type` varchar(30) NOT NULL,
    `amount` decimal(15,2) NOT NULL,
    `event_at` datetime NOT NULL,
    `processed` tinyint(1) NOT NULL DEFAULT 0,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_ledger_order` (`order_number`),
    KEY `idx_ledger_seller` (`seller_id`),
    KEY `idx_ledger_processed` (`processed`),
    KEY `idx_ledger_event_at` (`event_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
