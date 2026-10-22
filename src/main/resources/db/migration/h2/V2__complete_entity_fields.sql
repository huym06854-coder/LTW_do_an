ALTER TABLE branches ADD latitude DECIMAL(9,6);
ALTER TABLE branches ADD longitude DECIMAL(9,6);
ALTER TABLE product_images ADD cloudinary_public_id VARCHAR(255);
ALTER TABLE product_specifications ADD sort_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE carts ADD branch_id BIGINT;

ALTER TABLE orders ADD completed_at TIMESTAMP(6);
ALTER TABLE orders ADD version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE transfers ADD version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE coupons ADD version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE user_addresses ADD default_user_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN is_default THEN user_id ELSE NULL END);

ALTER TABLE product_images ADD primary_product_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN is_primary THEN product_id ELSE NULL END);
