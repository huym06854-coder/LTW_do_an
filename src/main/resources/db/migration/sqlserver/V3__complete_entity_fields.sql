ALTER TABLE branches
    ADD latitude DECIMAL(9,6) NULL;

ALTER TABLE branches
    ADD longitude DECIMAL(9,6) NULL;

ALTER TABLE product_images
    ADD cloudinary_public_id VARCHAR(255) NULL;

ALTER TABLE product_specifications
    ADD sort_order INT NOT NULL CONSTRAINT df_product_specifications_sort_order DEFAULT 0;

ALTER TABLE carts
    ADD branch_id BIGINT NULL;

ALTER TABLE orders
    ADD completed_at DATETIME2(6) NULL,
        version BIGINT NOT NULL CONSTRAINT df_orders_version DEFAULT 0;

ALTER TABLE transfers
    ADD version BIGINT NOT NULL CONSTRAINT df_transfers_version DEFAULT 0;

ALTER TABLE coupons
    ADD version BIGINT NOT NULL CONSTRAINT df_coupons_version DEFAULT 0;
