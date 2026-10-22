ALTER TABLE orders ADD checkout_token VARCHAR(64);
CREATE UNIQUE INDEX uk_orders_checkout_token ON orders(checkout_token);
ALTER TABLE payments ADD confirmed_by BIGINT;
ALTER TABLE payments ADD refunded_at TIMESTAMP;
ALTER TABLE payments ADD CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(id);
