-- Add application review fields to sellers table
ALTER TABLE sellers ADD COLUMN reason TEXT;
ALTER TABLE sellers ADD COLUMN reviewed_by VARCHAR(255);
ALTER TABLE sellers ADD COLUMN review_comment TEXT;
ALTER TABLE sellers ADD COLUMN reviewed_at TIMESTAMPTZ;
