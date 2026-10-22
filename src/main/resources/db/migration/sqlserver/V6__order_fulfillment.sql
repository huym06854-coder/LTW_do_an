ALTER TABLE orders ADD checkout_token VARCHAR(64) NULL;
EXEC(N'CREATE UNIQUE INDEX uk_orders_checkout_token ON dbo.orders(checkout_token) WHERE checkout_token IS NOT NULL');
ALTER TABLE payments ADD confirmed_by BIGINT NULL;
ALTER TABLE payments ADD refunded_at DATETIME2(6) NULL;
ALTER TABLE payments ADD CONSTRAINT fk_payments_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(id);
