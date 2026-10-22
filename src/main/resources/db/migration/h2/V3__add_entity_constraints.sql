ALTER TABLE branches ADD CONSTRAINT ck_branches_latitude
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90);
ALTER TABLE branches ADD CONSTRAINT ck_branches_longitude
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);

ALTER TABLE carts ADD CONSTRAINT fk_carts_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

CREATE UNIQUE INDEX uk_user_addresses_one_default ON user_addresses (default_user_id);
CREATE UNIQUE INDEX uk_product_images_one_primary ON product_images (primary_product_id);
