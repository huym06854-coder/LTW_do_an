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
