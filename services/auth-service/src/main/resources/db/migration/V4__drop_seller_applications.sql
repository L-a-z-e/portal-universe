-- Seller application workflow moved to seller-service
DROP TRIGGER IF EXISTS trg_seller_applications_updated_at ON seller_applications;
DROP TABLE IF EXISTS seller_applications;
