CREATE TABLE branches (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(500) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_branches_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_branches_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_branches_code UNIQUE (code)
);

CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_roles_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_roles_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE categories (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    parent_id BIGINT,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_categories_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_categories_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE NO ACTION
);

CREATE TABLE brands (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    logo_url VARCHAR(1000),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_brands_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_brands_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_brands_name UNIQUE (name)
);

CREATE TABLE users (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
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
    name VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
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
    recipient_name VARCHAR(160) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    province VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    ward VARCHAR(120) NOT NULL,
    address_detail VARCHAR(500) NOT NULL,
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
    is_primary BIT NOT NULL CONSTRAINT df_product_images_is_primary DEFAULT 0,
    sort_order INT NOT NULL CONSTRAINT df_product_images_sort_order DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_product_images_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_product_images_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE product_specifications (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    spec_name VARCHAR(120) NOT NULL,
    spec_value VARCHAR(500) NOT NULL,
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
    note VARCHAR(1000),
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
    comment VARCHAR(2000),
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
    note VARCHAR(1000),
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
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_carts_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_carts_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
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
    description VARCHAR(500),
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(19,2) NOT NULL,
    minimum_order_amount DECIMAL(19,2),
    maximum_discount_amount DECIMAL(19,2),
    starts_at DATETIME2(6) NOT NULL,
    expires_at DATETIME2(6) NOT NULL,
    usage_limit INT,
    used_count INT NOT NULL CONSTRAINT df_coupons_used_count DEFAULT 0,
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
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    coupon_id BIGINT,
    status VARCHAR(20) NOT NULL,
    delivery_method VARCHAR(30) NOT NULL,
    recipient_name VARCHAR(160) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    shipping_address VARCHAR(1000),
    subtotal DECIMAL(19,2) NOT NULL,
    shipping_fee DECIMAL(19,2) NOT NULL,
    discount_amount DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    note VARCHAR(1000),
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
    product_name_snapshot VARCHAR(200) NOT NULL,
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
    note VARCHAR(1000),
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
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_payments_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_payments_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_payments_transaction_id ON payments (transaction_id) WHERE transaction_id IS NOT NULL;

CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    read_at DATETIME2(6),
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_notifications_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_notifications_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

INSERT INTO roles (name, description) VALUES ('ADMIN', 'Quản trị viên hệ thống');
INSERT INTO roles (name, description) VALUES ('MANAGER', 'Quản lý chi nhánh');
INSERT INTO roles (name, description) VALUES ('STAFF', 'Nhân viên chi nhánh');
INSERT INTO roles (name, description) VALUES ('CUSTOMER', 'Khách hàng');
