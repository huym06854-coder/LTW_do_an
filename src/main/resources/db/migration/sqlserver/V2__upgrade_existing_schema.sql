-- Upgrade an existing TechStore schema before the new Flyway history takes over.
-- V1 runs only on an empty database; the old history table and application data remain intact.

-- Bring databases that stopped at an older legacy migration up to the final schema.
-- Databases already at V6/V7 skip these guarded changes.
IF COL_LENGTH('dbo.branches', 'latitude') IS NULL
    EXEC(N'ALTER TABLE branches ADD latitude DECIMAL(9,6) NULL');
IF COL_LENGTH('dbo.branches', 'longitude') IS NULL
    EXEC(N'ALTER TABLE branches ADD longitude DECIMAL(9,6) NULL');
IF COL_LENGTH('dbo.product_images', 'cloudinary_public_id') IS NULL
    EXEC(N'ALTER TABLE product_images ADD cloudinary_public_id VARCHAR(255) NULL');
IF COL_LENGTH('dbo.product_specifications', 'sort_order') IS NULL
    EXEC(N'ALTER TABLE product_specifications ADD sort_order INT NOT NULL CONSTRAINT df_product_specifications_sort_order DEFAULT 0');
IF COL_LENGTH('dbo.carts', 'branch_id') IS NULL
    EXEC(N'ALTER TABLE carts ADD branch_id BIGINT NULL');
IF COL_LENGTH('dbo.orders', 'completed_at') IS NULL
    EXEC(N'ALTER TABLE orders ADD completed_at DATETIME2(6) NULL');
IF COL_LENGTH('dbo.orders', 'version') IS NULL
    EXEC(N'ALTER TABLE orders ADD version BIGINT NOT NULL CONSTRAINT df_orders_version DEFAULT 0');
IF COL_LENGTH('dbo.transfers', 'version') IS NULL
    EXEC(N'ALTER TABLE transfers ADD version BIGINT NOT NULL CONSTRAINT df_transfers_version DEFAULT 0');
IF COL_LENGTH('dbo.coupons', 'version') IS NULL
    EXEC(N'ALTER TABLE coupons ADD version BIGINT NOT NULL CONSTRAINT df_coupons_version DEFAULT 0');
IF COL_LENGTH('dbo.orders', 'checkout_token') IS NULL
    EXEC(N'ALTER TABLE orders ADD checkout_token VARCHAR(64) NULL');
IF COL_LENGTH('dbo.payments', 'confirmed_by') IS NULL
    EXEC(N'ALTER TABLE payments ADD confirmed_by BIGINT NULL');
IF COL_LENGTH('dbo.payments', 'refunded_at') IS NULL
    EXEC(N'ALTER TABLE payments ADD refunded_at DATETIME2(6) NULL');

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_branches_latitude' AND parent_object_id = OBJECT_ID('dbo.branches'))
    EXEC(N'ALTER TABLE branches ADD CONSTRAINT ck_branches_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90)');
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_branches_longitude' AND parent_object_id = OBJECT_ID('dbo.branches'))
    EXEC(N'ALTER TABLE branches ADD CONSTRAINT ck_branches_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)');
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_carts_branch' AND parent_object_id = OBJECT_ID('dbo.carts'))
    EXEC(N'ALTER TABLE carts ADD CONSTRAINT fk_carts_branch FOREIGN KEY (branch_id) REFERENCES branches (id)');
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_payments_confirmed_by' AND parent_object_id = OBJECT_ID('dbo.payments'))
    EXEC(N'ALTER TABLE payments ADD CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id)');
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_user_addresses_one_default' AND object_id = OBJECT_ID('dbo.user_addresses'))
    EXEC(N'CREATE UNIQUE INDEX uk_user_addresses_one_default ON user_addresses (user_id) WHERE is_default = 1');
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_product_images_one_primary' AND object_id = OBJECT_ID('dbo.product_images'))
    EXEC(N'CREATE UNIQUE INDEX uk_product_images_one_primary ON product_images (product_id) WHERE is_primary = 1');
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_orders_checkout_token' AND object_id = OBJECT_ID('dbo.orders'))
    EXEC(N'CREATE UNIQUE INDEX uk_orders_checkout_token ON orders (checkout_token) WHERE checkout_token IS NOT NULL');

-- Keep user-facing product text in Unicode. Leave identifiers, codes and URLs as VARCHAR.
-- Drop only the unique constraints that depend on columns being changed, then recreate them.
ALTER TABLE categories DROP CONSTRAINT uk_categories_name;
ALTER TABLE brands DROP CONSTRAINT uk_brands_name;
ALTER TABLE product_specifications DROP CONSTRAINT uk_product_specification;

ALTER TABLE categories ALTER COLUMN name NVARCHAR(120) NOT NULL;
ALTER TABLE categories ALTER COLUMN description NVARCHAR(1000) NULL;
ALTER TABLE brands ALTER COLUMN name NVARCHAR(120) NOT NULL;
ALTER TABLE brands ALTER COLUMN description NVARCHAR(1000) NULL;
ALTER TABLE products ALTER COLUMN name NVARCHAR(200) NOT NULL;
ALTER TABLE products ALTER COLUMN description NVARCHAR(4000) NULL;
ALTER TABLE product_specifications ALTER COLUMN spec_name NVARCHAR(120) NOT NULL;
ALTER TABLE product_specifications ALTER COLUMN spec_value NVARCHAR(500) NOT NULL;
ALTER TABLE order_items ALTER COLUMN product_name_snapshot NVARCHAR(200) NOT NULL;

ALTER TABLE categories ADD CONSTRAINT uk_categories_name UNIQUE (name);
ALTER TABLE brands ADD CONSTRAINT uk_brands_name UNIQUE (name);
ALTER TABLE product_specifications ADD CONSTRAINT uk_product_specification UNIQUE (product_id, spec_name);

-- VARCHAR could already have replaced Vietnamese characters with '?' or mojibake.
-- Repair only recognizably damaged values belonging to the original sample SKUs;
-- leave edited product data and every other product untouched.
UPDATE p
SET name = CASE WHEN p.name LIKE N'%?%' OR p.name LIKE N'%Ã%' OR p.name LIKE N'%' + NCHAR(65533) + N'%'
                THEN sample.correct_name ELSE p.name END,
    description = CASE WHEN p.description LIKE N'%?%' OR p.description LIKE N'%Ã%'
                      OR p.description LIKE N'%' + NCHAR(65533) + N'%'
                       THEN sample.correct_description ELSE p.description END
FROM products p
JOIN (VALUES
    ('LAP-TD-OFFICE-14', N'Laptop Tech Demo Office 14 - Thiết bị văn phòng', N'Laptop văn phòng Intel Core i5, RAM 8GB, SSD 256GB.'),
    ('LAP-ASUS-GAMING-15', N'Laptop Tech Demo Gaming 15 - Hiệu năng cao', N'Laptop gaming Intel Core i7, RAM 16GB, SSD 512GB.'),
    ('LAP-LENOVO-AIR-13', N'Laptop Tech Demo Air 13 - Thiết kế mỏng nhẹ', N'Laptop mỏng nhẹ AMD Ryzen 5, RAM 16GB, SSD 512GB.'),
    ('LAP-DELL-STUDENT-14', N'Laptop Tech Demo Student 14 - Học tập', N'Laptop học tập Intel Core i5, RAM 8GB, SSD 256GB.'),
    ('MON-LG-24-FHD', N'Màn hình Tech Demo 24 inch Full HD', N'Màn hình 24 inch độ phân giải Full HD.'),
    ('UPS-TD-BACKUP', N'Bộ lưu điện Tech Demo - Nguồn dự phòng', N'Bộ lưu điện dành cho máy tính văn phòng.'),
    ('CAM-TD-OUTDOOR', N'Camera Tech Demo ngoài trời - Giám sát an ninh', N'Camera giám sát ngoài trời.'),
    ('NET-TPLINK-WIFI', N'Router Tech Demo Wi-Fi - Bộ phát mạng', N'Router Wi-Fi dành cho gia đình và văn phòng.'),
    ('ACC-TD-KEYBOARD', N'Bàn phím cơ Tech Demo - Phụ kiện máy tính', N'Bàn phím cơ dành cho gaming và văn phòng.'),
    ('LAP-MSI-CREATOR-16', N'Laptop Tech Demo Creator 16 - Đồ họa chuyên nghiệp', N'Laptop đồ họa AMD Ryzen 7, RAM 32GB, SSD 1TB.')
) sample(sku, correct_name, correct_description) ON sample.sku = p.sku;

-- Match sample categories by their seeded products, without changing other categories.
UPDATE c SET name = N'Màn hình'
FROM categories c JOIN products p ON p.category_id = c.id AND p.sku = 'MON-LG-24-FHD'
WHERE (c.name LIKE N'%?%' OR c.name LIKE N'%Ã%' OR c.name LIKE N'%' + NCHAR(65533) + N'%')
  AND NOT EXISTS (SELECT 1 FROM categories other WHERE other.name = N'Màn hình' AND other.id <> c.id);

UPDATE c SET name = N'Mạng'
FROM categories c JOIN products p ON p.category_id = c.id AND p.sku = 'NET-TPLINK-WIFI'
WHERE (c.name LIKE N'%?%' OR c.name LIKE N'%Ã%' OR c.name LIKE N'%' + NCHAR(65533) + N'%')
  AND NOT EXISTS (SELECT 1 FROM categories other WHERE other.name = N'Mạng' AND other.id <> c.id);

UPDATE c SET name = N'Phụ kiện'
FROM categories c JOIN products p ON p.category_id = c.id AND p.sku = 'ACC-TD-KEYBOARD'
WHERE (c.name LIKE N'%?%' OR c.name LIKE N'%Ã%' OR c.name LIKE N'%' + NCHAR(65533) + N'%')
  AND NOT EXISTS (SELECT 1 FROM categories other WHERE other.name = N'Phụ kiện' AND other.id <> c.id);

-- Keep user-entered Vietnamese text in Unicode. Codes, email, phone, URL and enum values stay VARCHAR.
ALTER TABLE branches ALTER COLUMN name NVARCHAR(160) NOT NULL;
ALTER TABLE branches ALTER COLUMN address NVARCHAR(500) NOT NULL;
ALTER TABLE roles ALTER COLUMN description NVARCHAR(500) NULL;
ALTER TABLE users ALTER COLUMN full_name NVARCHAR(160) NOT NULL;
ALTER TABLE user_addresses ALTER COLUMN recipient_name NVARCHAR(160) NOT NULL;
ALTER TABLE user_addresses ALTER COLUMN province NVARCHAR(120) NOT NULL;
ALTER TABLE user_addresses ALTER COLUMN district NVARCHAR(120) NOT NULL;
ALTER TABLE user_addresses ALTER COLUMN ward NVARCHAR(120) NOT NULL;
ALTER TABLE user_addresses ALTER COLUMN address_detail NVARCHAR(500) NOT NULL;
ALTER TABLE stock_logs ALTER COLUMN note NVARCHAR(1000) NULL;
ALTER TABLE reviews ALTER COLUMN comment NVARCHAR(2000) NULL;
ALTER TABLE transfers ALTER COLUMN note NVARCHAR(1000) NULL;
ALTER TABLE coupons ALTER COLUMN description NVARCHAR(500) NULL;
ALTER TABLE orders ALTER COLUMN recipient_name NVARCHAR(160) NOT NULL;
ALTER TABLE orders ALTER COLUMN shipping_address NVARCHAR(1000) NULL;
ALTER TABLE orders ALTER COLUMN note NVARCHAR(1000) NULL;
ALTER TABLE order_status_history ALTER COLUMN note NVARCHAR(1000) NULL;
ALTER TABLE notifications ALTER COLUMN title NVARCHAR(160) NOT NULL;
ALTER TABLE notifications ALTER COLUMN message NVARCHAR(2000) NOT NULL;

-- These indexes match catalog paging, customer order history and completed revenue queries.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_products_status_category_price' AND object_id = OBJECT_ID('dbo.products'))
    EXEC(N'CREATE INDEX ix_products_status_category_price ON products (status, category_id, price, id)');
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_orders_user_created_at' AND object_id = OBJECT_ID('dbo.orders'))
    EXEC(N'CREATE INDEX ix_orders_user_created_at ON orders (user_id, created_at DESC)');
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_orders_status_completed_at' AND object_id = OBJECT_ID('dbo.orders'))
    EXEC(N'CREATE INDEX ix_orders_status_completed_at ON orders (status, completed_at, branch_id)');

-- Seed accounts created before the password bootstrap used non-hashed passwords.
UPDATE users
SET status = 'LOCKED', password = 'RESET_REQUIRED_NON_HASHED_PASSWORD'
WHERE password NOT LIKE '$2a$%'
  AND password NOT LIKE '$2b$%'
  AND password NOT LIKE '$2y$%';

-- Changing VARCHAR to NVARCHAR preserves existing bytes; it cannot recover accents
-- that were lost when the original demo branches were inserted. Only the four
-- known seed codes are eligible. Keep independently edited branch details.
UPDATE branch
SET name = CASE
        WHEN branch.name COLLATE Latin1_General_100_BIN2 =
             CONVERT(NVARCHAR(160), CONVERT(VARCHAR(160), seed.correct_name)) COLLATE Latin1_General_100_BIN2
          OR (branch.name LIKE N'TechDemo %' AND
              (branch.name LIKE N'%?%' OR branch.name LIKE N'%Ã%' OR
               branch.name LIKE N'%áº%' OR branch.name LIKE N'%á»%' OR
               branch.name LIKE N'%' + NCHAR(65533) + N'%'))
        THEN seed.correct_name ELSE branch.name END,
    address = CASE
        WHEN branch.address COLLATE Latin1_General_100_BIN2 =
             CONVERT(NVARCHAR(500), CONVERT(VARCHAR(500), seed.correct_address)) COLLATE Latin1_General_100_BIN2
          OR (branch.address LIKE seed.address_prefix + N'%' AND
              (branch.address LIKE N'%?%' OR branch.address LIKE N'%Ã%' OR
               branch.address LIKE N'%áº%' OR branch.address LIKE N'%á»%' OR
               branch.address LIKE N'%' + NCHAR(65533) + N'%'))
        THEN seed.correct_address ELSE branch.address END
FROM branches AS branch
JOIN (VALUES
    ('CN001', N'TechDemo Quận 1', N'128 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh', N'128 '),
    ('CN002', N'TechDemo Hà Nội', N'42 Cầu Giấy, Hà Nội', N'42 '),
    ('CN003', N'TechDemo Đà Nẵng', N'21 Nguyễn Văn Linh, Đà Nẵng', N'21 '),
    ('CN004', N'TechDemo Thủ Đức', N'15 Võ Văn Ngân, TP. Thủ Đức', N'15 ')
) AS seed(code, correct_name, correct_address, address_prefix) ON seed.code = branch.code;
