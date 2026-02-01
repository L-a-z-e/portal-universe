-- Product.price: DOUBLE → DECIMAL(19,2) for precise monetary calculations
ALTER TABLE products MODIFY COLUMN price DECIMAL(19,2) NOT NULL;
