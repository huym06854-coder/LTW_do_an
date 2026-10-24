-- Complete SQL Server schema and demo data for an empty database. Existing schemas use V2.

CREATE TABLE branches (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name NVARCHAR(160) NOT NULL,
    address NVARCHAR(500) NOT NULL,
    phone VARCHAR(30),
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_branches_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_branches_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_branches_code UNIQUE (code),
    CONSTRAINT ck_branches_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_branches_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    description NVARCHAR(500),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_roles_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_roles_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE categories (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name NVARCHAR(120) NOT NULL,
    description NVARCHAR(1000),
    parent_id BIGINT,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_categories_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_categories_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE NO ACTION
);

CREATE TABLE brands (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name NVARCHAR(120) NOT NULL,
    description NVARCHAR(1000),
    logo_url VARCHAR(1000),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_brands_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_brands_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_brands_name UNIQUE (name)
);

CREATE TABLE users (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    full_name NVARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    status VARCHAR(20) NOT NULL,
    role_id BIGINT NOT NULL,
    branch_id BIGINT,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_users_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_users_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
);

CREATE TABLE products (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    sku VARCHAR(80) NOT NULL,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(4000),
    price DECIMAL(19,2) NOT NULL,
    cost_price DECIMAL(19,2),
    status VARCHAR(20) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_products_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_products_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_cost_price CHECK (cost_price IS NULL OR cost_price >= 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id)
);

CREATE TABLE user_addresses (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name NVARCHAR(160) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    province NVARCHAR(120) NOT NULL,
    district NVARCHAR(120) NOT NULL,
    ward NVARCHAR(120) NOT NULL,
    address_detail NVARCHAR(500) NOT NULL,
    is_default BIT NOT NULL CONSTRAINT df_user_addresses_is_default DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_user_addresses_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_user_addresses_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME2(6) NOT NULL,
    used_at DATETIME2(6),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_password_reset_tokens_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_password_reset_tokens_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE product_images (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    cloudinary_public_id VARCHAR(255),
    is_primary BIT NOT NULL CONSTRAINT df_product_images_is_primary DEFAULT 0,
    sort_order INT NOT NULL CONSTRAINT df_product_images_sort_order DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_product_images_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_product_images_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE product_specifications (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    spec_name NVARCHAR(120) NOT NULL,
    spec_value NVARCHAR(500) NOT NULL,
    sort_order INT NOT NULL CONSTRAINT df_product_specifications_sort_order DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_product_specifications_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_product_specifications_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_product_specification UNIQUE (product_id, spec_name),
    CONSTRAINT fk_product_specifications_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE branch_stocks (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CONSTRAINT df_branch_stocks_quantity DEFAULT 0,
    version BIGINT NOT NULL CONSTRAINT df_branch_stocks_version DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_branch_stocks_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_branch_stocks_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_branch_stock_branch_product UNIQUE (branch_id, product_id),
    CONSTRAINT ck_branch_stock_quantity CHECK (quantity >= 0),
    CONSTRAINT fk_branch_stocks_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_branch_stocks_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE stock_logs (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity_change INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    performed_by BIGINT,
    reference_code VARCHAR(80),
    note NVARCHAR(1000),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_stock_logs_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_stock_logs_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_stock_logs_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_stock_logs_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_logs_user FOREIGN KEY (performed_by) REFERENCES users (id)
);

CREATE TABLE reviews (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment NVARCHAR(2000),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_reviews_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_reviews_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_review_user_product UNIQUE (user_id, product_id),
    CONSTRAINT ck_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE transfers (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    source_branch_id BIGINT NOT NULL,
    destination_branch_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    approved_by BIGINT,
    version BIGINT NOT NULL CONSTRAINT df_transfers_version DEFAULT 0,
    note NVARCHAR(1000),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_transfers_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_transfers_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_transfers_code UNIQUE (code),
    CONSTRAINT ck_transfer_distinct_branches CHECK (source_branch_id <> destination_branch_id),
    CONSTRAINT fk_transfers_source_branch FOREIGN KEY (source_branch_id) REFERENCES branches (id),
    CONSTRAINT fk_transfers_destination_branch FOREIGN KEY (destination_branch_id) REFERENCES branches (id),
    CONSTRAINT fk_transfers_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_transfers_approver FOREIGN KEY (approved_by) REFERENCES users (id)
);

CREATE TABLE transfer_items (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_transfer_items_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_transfer_items_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_transfer_item_product UNIQUE (transfer_id, product_id),
    CONSTRAINT ck_transfer_item_quantity CHECK (quantity > 0),
    CONSTRAINT fk_transfer_items_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE carts (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    branch_id BIGINT,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_carts_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_carts_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_carts_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
);

CREATE TABLE cart_items (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_cart_items_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_cart_items_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_cart_item_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE coupons (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    description NVARCHAR(500),
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(19,2) NOT NULL,
    minimum_order_amount DECIMAL(19,2),
    maximum_discount_amount DECIMAL(19,2),
    starts_at DATETIME2(6) NOT NULL,
    expires_at DATETIME2(6) NOT NULL,
    usage_limit INT,
    used_count INT NOT NULL CONSTRAINT df_coupons_used_count DEFAULT 0,
    version BIGINT NOT NULL CONSTRAINT df_coupons_version DEFAULT 0,
    active BIT NOT NULL CONSTRAINT df_coupons_active DEFAULT 1,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_coupons_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_coupons_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupon_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_coupon_usage CHECK (usage_limit IS NULL OR usage_limit >= 0)
);

CREATE TABLE orders (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    checkout_token VARCHAR(64),
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    coupon_id BIGINT,
    status VARCHAR(20) NOT NULL,
    delivery_method VARCHAR(30) NOT NULL,
    recipient_name NVARCHAR(160) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    shipping_address NVARCHAR(1000),
    subtotal DECIMAL(19,2) NOT NULL,
    shipping_fee DECIMAL(19,2) NOT NULL,
    discount_amount DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    note NVARCHAR(1000),
    completed_at DATETIME2(6),
    version BIGINT NOT NULL CONSTRAINT df_orders_version DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_orders_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_orders_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT ck_order_amounts CHECK (subtotal >= 0 AND shipping_fee >= 0 AND discount_amount >= 0 AND total_amount >= 0),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id)
);

CREATE TABLE order_items (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name_snapshot NVARCHAR(200) NOT NULL,
    sku_snapshot VARCHAR(80) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_order_items_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_order_items_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_order_item_order_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_item_prices CHECK (unit_price >= 0 AND subtotal >= 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE order_status_history (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT,
    note NVARCHAR(1000),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_order_status_history_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_order_status_history_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE TABLE payments (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(120),
    paid_at DATETIME2(6),
    confirmed_by BIGINT,
    refunded_at DATETIME2(6),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_payments_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_payments_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_orders_checkout_token ON orders (checkout_token) WHERE checkout_token IS NOT NULL;
CREATE UNIQUE INDEX uk_user_addresses_one_default ON user_addresses (user_id) WHERE is_default = 1;
CREATE UNIQUE INDEX uk_product_images_one_primary ON product_images (product_id) WHERE is_primary = 1;

CREATE UNIQUE INDEX uk_payments_transaction_id ON payments (transaction_id) WHERE transaction_id IS NOT NULL;

CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(160) NOT NULL,
    message NVARCHAR(2000) NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    read_at DATETIME2(6),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_notifications_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_notifications_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_products_status_category_price ON products (status, category_id, price, id);
CREATE INDEX ix_orders_user_created_at ON orders (user_id, created_at DESC);
CREATE INDEX ix_orders_status_completed_at ON orders (status, completed_at, branch_id);

INSERT INTO roles (name, description) VALUES ('ADMIN', N'Quản trị viên hệ thống');
INSERT INTO roles (name, description) VALUES ('MANAGER', N'Quản lý chi nhánh');
INSERT INTO roles (name, description) VALUES ('STAFF', N'Nhân viên chi nhánh');
INSERT INTO roles (name, description) VALUES ('CUSTOMER', N'Khách hàng');

/* Seed data mirrored from the current storefront and admin demo UI. */

INSERT INTO branches (code, name, address, phone, status)
VALUES
    ('CN001', N'TechDemo Quận 1', N'128 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh', '0901234567', 'ACTIVE'),
    ('CN002', N'TechDemo Hà Nội', N'42 Cầu Giấy, Hà Nội', '0902345678', 'ACTIVE'),
    ('CN003', N'TechDemo Đà Nẵng', N'21 Nguyễn Văn Linh, Đà Nẵng', '0903456789', 'ACTIVE'),
    ('CN004', N'TechDemo Thủ Đức', N'15 Võ Văn Ngân, TP. Thủ Đức', '0904567890', 'ACTIVE');

INSERT INTO categories (name, description)
VALUES
    (N'Laptop', N'Máy tính xách tay phục vụ học tập, văn phòng, gaming và đồ họa'),
    (N'Màn hình', N'Màn hình máy tính và thiết bị hiển thị'),
    (N'PC', N'Máy tính để bàn và thiết bị nguồn'),
    (N'Camera', N'Thiết bị camera và giám sát an ninh'),
    (N'Mạng', N'Thiết bị mạng và kết nối Internet'),
    (N'Phụ kiện', N'Phụ kiện máy tính và thiết bị ngoại vi');

INSERT INTO brands (name, description)
VALUES
    (N'Tech Demo', N'Thương hiệu sản phẩm mẫu của TechDemo'),
    (N'ASUS', N'Thiết bị máy tính ASUS'),
    (N'Lenovo', N'Thiết bị máy tính Lenovo'),
    (N'Dell', N'Thiết bị máy tính Dell'),
    (N'LG', N'Thiết bị hiển thị LG'),
    (N'TP-Link', N'Thiết bị mạng TP-Link'),
    (N'MSI', N'Thiết bị máy tính MSI');

INSERT INTO products (sku, name, description, price, cost_price, status, category_id, brand_id)
SELECT seed.sku, seed.product_name, seed.description, seed.price, seed.cost_price, seed.status,
       category.id, brand.id
FROM (VALUES
    ('LAP-TD-OFFICE-14', N'Laptop Tech Demo Office 14 - Thiết bị văn phòng', N'Laptop văn phòng Intel Core i5, RAM 8GB, SSD 256GB.', CAST(16990000 AS DECIMAL(19,2)), CAST(14500000 AS DECIMAL(19,2)), 'ACTIVE', N'Laptop', N'Tech Demo'),
    ('LAP-ASUS-GAMING-15', N'Laptop Tech Demo Gaming 15 - Hiệu năng cao', N'Laptop gaming Intel Core i7, RAM 16GB, SSD 512GB.', 25990000, 22500000, 'ACTIVE', N'Laptop', N'ASUS'),
    ('LAP-LENOVO-AIR-13', N'Laptop Tech Demo Air 13 - Thiết kế mỏng nhẹ', N'Laptop mỏng nhẹ AMD Ryzen 5, RAM 16GB, SSD 512GB.', 18990000, 16100000, 'ACTIVE', N'Laptop', N'Lenovo'),
    ('LAP-DELL-STUDENT-14', N'Laptop Tech Demo Student 14 - Học tập', N'Laptop học tập Intel Core i5, RAM 8GB, SSD 256GB.', 11990000, 10100000, 'ACTIVE', N'Laptop', N'Dell'),
    ('MON-LG-24-FHD', N'Màn hình Tech Demo 24 inch Full HD', N'Màn hình 24 inch độ phân giải Full HD.', 2990000, 2350000, 'ACTIVE', N'Màn hình', N'LG'),
    ('UPS-TD-BACKUP', N'Bộ lưu điện Tech Demo - Nguồn dự phòng', N'Bộ lưu điện dành cho máy tính văn phòng.', 2250000, 1800000, 'INACTIVE', N'PC', N'Tech Demo'),
    ('CAM-TD-OUTDOOR', N'Camera Tech Demo ngoài trời - Giám sát an ninh', N'Camera giám sát ngoài trời.', 1290000, 950000, 'ACTIVE', N'Camera', N'Tech Demo'),
    ('NET-TPLINK-WIFI', N'Router Tech Demo Wi-Fi - Bộ phát mạng', N'Router Wi-Fi dành cho gia đình và văn phòng.', 890000, 650000, 'ACTIVE', N'Mạng', N'TP-Link'),
    ('ACC-TD-KEYBOARD', N'Bàn phím cơ Tech Demo - Phụ kiện máy tính', N'Bàn phím cơ dành cho gaming và văn phòng.', 690000, 470000, 'ACTIVE', N'Phụ kiện', N'Tech Demo'),
    ('LAP-MSI-CREATOR-16', N'Laptop Tech Demo Creator 16 - Đồ họa chuyên nghiệp', N'Laptop đồ họa AMD Ryzen 7, RAM 32GB, SSD 1TB.', 32990000, 28900000, 'ACTIVE', N'Laptop', N'MSI')
) seed(sku, product_name, description, price, cost_price, status, category_name, brand_name)
JOIN categories category ON category.name = seed.category_name
JOIN brands brand ON brand.name = seed.brand_name;

INSERT INTO product_specifications (product_id, spec_name, spec_value)
SELECT product.id, specs.spec_name, specs.spec_value
FROM (VALUES
    ('LAP-TD-OFFICE-14', N'CPU', N'Intel Core i5'), ('LAP-TD-OFFICE-14', N'RAM', N'8GB'), ('LAP-TD-OFFICE-14', N'SSD', N'256GB'),
    ('LAP-ASUS-GAMING-15', N'CPU', N'Intel Core i7'), ('LAP-ASUS-GAMING-15', N'RAM', N'16GB'), ('LAP-ASUS-GAMING-15', N'SSD', N'512GB'),
    ('LAP-LENOVO-AIR-13', N'CPU', N'AMD Ryzen 5'), ('LAP-LENOVO-AIR-13', N'RAM', N'16GB'), ('LAP-LENOVO-AIR-13', N'SSD', N'512GB'),
    ('LAP-DELL-STUDENT-14', N'CPU', N'Intel Core i5'), ('LAP-DELL-STUDENT-14', N'RAM', N'8GB'), ('LAP-DELL-STUDENT-14', N'SSD', N'256GB'),
    ('LAP-MSI-CREATOR-16', N'CPU', N'AMD Ryzen 7'), ('LAP-MSI-CREATOR-16', N'RAM', N'32GB'), ('LAP-MSI-CREATOR-16', N'SSD', N'1TB')
) specs(sku, spec_name, spec_value)
JOIN products product ON product.sku = specs.sku;

INSERT INTO users (full_name, email, username, password, phone, status, role_id, branch_id)
SELECT employee.full_name, employee.email, employee.username, employee.password, employee.phone,
       employee.status, role.id, branch.id
FROM (VALUES
    (N'Nguyễn Minh Anh', 'minhanh@techdemo.vn', 'minhanh', 'CHANGE_ME', '0901234567', 'ACTIVE', 'MANAGER', 'CN001'),
    (N'Trần Quốc Bảo', 'quocbao@techdemo.vn', 'quocbao', 'CHANGE_ME', '0902345678', 'ACTIVE', 'MANAGER', 'CN002'),
    (N'Lê Thu Hà', 'thuha@techdemo.vn', 'thuha', 'CHANGE_ME', '0903456789', 'ACTIVE', 'MANAGER', 'CN003'),
    (N'Phạm Hoàng Long', 'hoanglong@techdemo.vn', 'hoanglong', 'CHANGE_ME', '0904567890', 'ACTIVE', 'MANAGER', 'CN004'),
    (N'Võ Thanh Tùng', 'tung.vo@techdemo.vn', 'thanhtung', 'CHANGE_ME', '0912111222', 'ACTIVE', 'STAFF', 'CN001'),
    (N'Đặng Ngọc Mai', 'mai.dang@techdemo.vn', 'ngocmai', 'CHANGE_ME', '0913222333', 'ACTIVE', 'STAFF', 'CN001'),
    (N'Phan Đức Huy', 'huy.phan@techdemo.vn', 'duchuy', 'CHANGE_ME', '0914333444', 'ACTIVE', 'STAFF', 'CN002'),
    (N'Bùi Khánh Linh', 'linh.bui@techdemo.vn', 'khanhlinh', 'CHANGE_ME', '0915444555', 'LOCKED', 'STAFF', 'CN003')
) employee(full_name, email, username, password, phone, status, role_name, branch_code)
JOIN roles role ON role.name = employee.role_name
JOIN branches branch ON branch.code = employee.branch_code;

INSERT INTO branch_stocks (branch_id, product_id, quantity)
SELECT branch.id, product.id,
       CASE
           WHEN product.sku = 'UPS-TD-BACKUP' THEN 0
           ELSE seed.quantity
       END
FROM branches branch
CROSS JOIN products product
CROSS APPLY (VALUES (
    CASE branch.code WHEN 'CN001' THEN 25 WHEN 'CN002' THEN 18 WHEN 'CN003' THEN 12 ELSE 10 END
)) seed(quantity);

INSERT INTO coupons
    (code, description, discount_type, discount_value, minimum_order_amount,
     maximum_discount_amount, starts_at, expires_at, usage_limit, used_count, active)
VALUES
    ('WELCOME10', N'Giảm 10% cho đơn hàng đầu tiên', 'PERCENTAGE', 10, 1000000, 1000000,
     '2026-01-01T00:00:00', '2027-12-31T23:59:59', 1000, 0, 1),
    ('TECH500K', N'Giảm trực tiếp 500.000đ cho đơn từ 10 triệu', 'FIXED_AMOUNT', 500000, 10000000, 500000,
     '2026-01-01T00:00:00', '2027-12-31T23:59:59', 500, 0, 1);

-- Demo accounts created by older seed data used plain text passwords.
-- Require a secure password reset before those accounts can sign in.
UPDATE users
SET status = 'LOCKED', password = 'RESET_REQUIRED_NON_HASHED_PASSWORD'
WHERE password NOT LIKE '$2a$%'
  AND password NOT LIKE '$2b$%'
  AND password NOT LIKE '$2y$%';
