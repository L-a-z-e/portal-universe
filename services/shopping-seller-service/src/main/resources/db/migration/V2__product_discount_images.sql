-- ===================================================================
-- V2: Product 확장 - 할인가, 추천 상품, 다중 이미지
-- ===================================================================

-- 상품 테이블에 할인가, 추천 상품 컬럼 추가
ALTER TABLE `products`
  ADD COLUMN `discount_price` decimal(19,2) DEFAULT NULL AFTER `price`,
  ADD COLUMN `featured` tinyint(1) NOT NULL DEFAULT 0 AFTER `category`;

-- 상품 이미지 테이블
CREATE TABLE IF NOT EXISTS `product_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `alt_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_images_product_id` (`product_id`),
  KEY `idx_product_images_sort_order` (`product_id`, `sort_order`),
  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
