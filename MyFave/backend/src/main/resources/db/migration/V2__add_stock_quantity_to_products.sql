ALTER TABLE products ADD COLUMN stock_quantity INTEGER NOT NULL DEFAULT 0;
UPDATE products SET stock_quantity = CASE WHEN is_soldout THEN 0 ELSE 1 END;
