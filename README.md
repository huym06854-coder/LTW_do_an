# TechStore

Backend bán hàng xây dựng bằng Spring Boot, Spring MVC, Spring Security, Spring Data JPA và Thymeleaf. Ứng dụng lưu dữ liệu qua SQL Server và quản lý schema bằng Flyway.

## Yêu cầu

- JDK 21 trở lên
- SQL Server đang chạy và một database dành riêng cho project
- Maven Wrapper có sẵn trong repository

## Cấu hình và chạy

1. Sao chép `.env.example` thành `.env` nếu máy chưa có file `.env`.
2. Điền thông tin kết nối SQL Server vào `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Đặt `JWT_SECRET` thành chuỗi ngẫu nhiên tối thiểu 32 byte. Các cấu hình email và Cloudinary chỉ cần thiết khi dùng những chức năng tương ứng.
3. Từ thư mục project, chạy:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

Ứng dụng mặc định chạy tại `http://localhost:8080`. Flyway tự áp dụng migration theo database vendor khi khởi động. Các migration SQL Server nằm trong `src/main/resources/db/migration/sqlserver`; dữ liệu mẫu và cấu trúc hỗ trợ H2 nằm ở các thư mục tương ứng.

## Tạo tài khoản quản trị đầu tiên

Project không dùng tài khoản admin mặc định hoặc mật khẩu được hard-code. Tài khoản đầu tiên được tạo một lần bởi `AdminBootstrapService` khi ứng dụng khởi động và kết nối database thành công.

Trước lần khởi động đầu tiên, điền các biến `ADMIN_BOOTSTRAP_*` sau trong file `.env` bằng thông tin riêng của bạn. Để tương thích cấu hình cũ, service cũng nhận `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` và `ADMIN_FULL_NAME` nếu chưa có biến `ADMIN_BOOTSTRAP_*` tương ứng:

```dotenv
ADMIN_BOOTSTRAP_USERNAME=admin
ADMIN_BOOTSTRAP_EMAIL=admin@example.com
ADMIN_BOOTSTRAP_PASSWORD=THAY_BANG_MAT_KHAU_RIENG_TU_8_KY_TU_TRO_LEN
```

Mật khẩu phải dài ít nhất 8 ký tự. Nếu thông tin khởi tạo thiếu email hoặc mật khẩu không đạt độ dài, service ghi cảnh báo và bỏ qua việc tạo/kích hoạt admin để ứng dụng vẫn khởi động; khi đó cần bổ sung cấu hình hợp lệ để tạo tài khoản. Sau khi ứng dụng khởi động thành công, đăng nhập tại `/login` bằng username hoặc email đã cấu hình rồi mở `/admin`. Mật khẩu được lưu dưới dạng hash; service không ghi mật khẩu rõ vào log. Nếu username đã thuộc một admin đang hoạt động, service sẽ không đổi mật khẩu. Sau khi tạo thành công, xóa các biến bootstrap (cả tên mới và tên tương thích cũ) khỏi `.env` để tránh giữ thông tin khởi tạo lâu dài.

Không commit `.env` hoặc chia sẻ mật khẩu admin. Dùng `.env.example` làm mẫu và thay toàn bộ giá trị ví dụ trên máy của bạn.

## Các đường dẫn chính

- `/` — trang cửa hàng
- `/login`, `/register` — đăng nhập và đăng ký khách hàng
- `/products`, `/cart`, `/checkout`, `/orders` — mua hàng và quản lý đơn của khách hàng
- `/admin` — trang quản trị; các chức năng quản trị yêu cầu quyền ADMIN
- `/staff` — các chức năng nhân viên chi nhánh
- `/api/auth`, `/api/products`, `/api/cart`, `/api/orders`, `/api/stocks` — API

## Build

```powershell
.\mvnw.cmd test
```

Nếu IDE đang tự biên dịch và làm thay đổi `target` trong lúc Maven chạy, có thể tách thư mục kiểm thử:

```powershell
.\mvnw.cmd "-Dtechstore.build.directory=target-codex-verification" test
```

Đóng gói WAR:

```powershell
.\mvnw.cmd -DskipTests package
```

## Địa chỉ, thanh toán và xử lý đơn

- Sổ địa chỉ: `/profile?section=addresses`. Mỗi địa chỉ thuộc một tài khoản; các thao tác ghi được khóa theo tài khoản để bảo đảm chỉ có một địa chỉ mặc định. Đơn hàng lưu bản sao thông tin người nhận nên không đổi khi địa chỉ bị sửa/xóa.
- Checkout chỉ dùng chi nhánh đã kiểm tra trong giỏ hàng. Tạm tính, ưu đãi, phí vận chuyển và tổng thanh toán được tính ở backend; lỗi form giữ lại thông tin đã nhập. Mỗi phiên đặt hàng có mã chống tạo đơn trùng.
- `SHIPPING_FLAT_FEE` là phí giao tận nơi (VND), mặc định `0`. `SHIPPING_FREE_FROM` là ngưỡng miễn phí theo tạm tính, `0` nghĩa không áp dụng ngưỡng. Nhận tại cửa hàng luôn miễn phí. Điền biểu phí thực trước khi thu phí; phí được hiển thị ở bước xác nhận.
- Giao tận nơi: chờ xác nhận → xác nhận → chuẩn bị → giao hàng → hoàn tất. Nhận tại cửa hàng có bước **Sẵn sàng nhận hàng** trước khi hoàn tất. Hoàn tất COD cần nhân viên xác nhận đã thu đủ tiền; hệ thống ghi người xác nhận và thời điểm.
- Giao thất bại không hoàn kho. Đơn phải qua **Đang hoàn hàng → Đã nhận hàng hoàn** mới cộng lại kho. Đơn đã thu tiền chỉ được ghi nhận hoàn tiền bằng xác nhận thủ công của quản lý/admin sau khi hàng hoàn về. Các thao tác không thay thế giao dịch chuyển tiền thực tế.
- Khách xem đơn của mình tại `/orders`; nhân viên xem đơn thuộc chi nhánh ở `/staff/orders`; admin xem toàn hệ thống ở `/admin/orders`. Các thao tác hiển thị theo trạng thái hợp lệ. Thông báo đơn mới gửi đến nhân viên/quản lý chi nhánh và admin qua WebSocket sau khi transaction thành công; trình duyệt chỉ được đăng ký kênh cá nhân, không được nghe kênh chung hoặc tự gửi thông báo.

Các migration bổ sung là SQL Server `V6__order_fulfillment.sql` và H2 `V4__order_fulfillment.sql`; không sửa migration đã áp dụng. Các nút yêu thích/đăng ký nhận tin chưa có dịch vụ lưu trữ được ẩn hoặc ghi rõ sắp ra mắt.

Khi chia sẻ mã nguồn, loại `.env`, `.git`, `target` và các file chứa credential khỏi ZIP. Nếu credential thật từng bị chia sẻ, thay chúng tại dịch vụ tương ứng.
