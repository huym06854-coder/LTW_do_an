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

Schema nền được tạo bởi V1; SQL Server V2 nâng cấp schema có sẵn. Lịch sử phân công nhân sự và chuyển kho được thêm bằng SQL Server `V3__operations_audit_history.sql` và H2 `V2__operations_audit_history.sql`. Không chỉnh sửa migration đã áp dụng. Các nút yêu thích/đăng ký nhận tin chưa có dịch vụ lưu trữ được ẩn hoặc ghi rõ sắp ra mắt.

Một số database đã áp dụng `V7__nationalize_product_text.sql` trước khi bổ sung V3. Cấu hình `spring.flyway.out-of-order=true` cho phép Flyway chạy V3 còn thiếu dù phiên bản cao nhất là 7; Flyway vẫn kiểm tra migration và ghi lịch sử để không chạy V3 lần thứ hai. Không xóa bảng `flyway_schema_history_v2`, không xóa database và không bỏ qua migration V3. Migration mới tiếp theo cần có số lớn hơn phiên bản đã triển khai cao nhất (hiện tại là 7).

Khi chia sẻ mã nguồn, loại `.env`, `.git`, `target` và các file chứa credential khỏi ZIP. Nếu credential thật từng bị chia sẻ, thay chúng tại dịch vụ tương ứng.

## Vận hành Admin – Chi nhánh – Nhân sự

- `/admin/branches`: số quản lý, nhân viên, đơn chờ và phiếu chờ duyệt theo chi nhánh.
- `/admin/branches/{id}?tab=overview|employees|orders|stock|transfers`: tổng quan, nhân sự, đơn hàng, kho và chuyển kho của một chi nhánh.
- `/admin/users`: nhóm Nhân sự / Khách hàng / Tất cả, tìm kiếm và lọc theo vai trò, chi nhánh, trạng thái; mỗi trang 15 tài khoản.
- `/admin/users/{id}`: xem hồ sơ, đổi STAFF ↔ MANAGER, chuyển chi nhánh, xem 50 thay đổi phân công/trạng thái gần nhất. Luồng này không nâng quyền CUSTOMER hoặc ADMIN.
- `/staff`: dashboard thực, tên chi nhánh, công việc chờ và cảnh báo tồn kho; tài khoản chưa phân công hoặc chi nhánh tạm dừng có thông báo riêng.
- `/staff/stock`: nhân viên và quản lý đều được xem/điều chỉnh kho của chi nhánh mình theo chính sách hiện tại. Mỗi điều chỉnh cần lý do và được lưu StockLog.
- `/staff/transfers`: nhân viên/quản lý tạo yêu cầu từ chi nhánh của mình; Manager nguồn hoặc Admin duyệt; nguồn xuất; đích nhận. Nhấn mã phiếu để xem người yêu cầu và lịch sử xử lý.

Session web được kiểm tra lại trạng thái và vai trò từ database trước mỗi request động. Khóa hoặc ngừng hoạt động tài khoản sẽ vô hiệu session ở lần truy cập tiếp theo, kể cả ở khu vực hồ sơ hay quản trị. Đổi vai trò cập nhật quyền trên phiên hiện tại; đổi chi nhánh áp dụng phạm vi mới mà không cần đăng nhập lại. JWT tiếp tục kiểm tra tài khoản ở mỗi request.

Chi nhánh chỉ được ngừng hoạt động khi không còn đơn chưa kết thúc (ngoài COMPLETED, CANCELLED, RETURNED) và không còn phiếu chuyển kho mở. Chi nhánh ngừng hoạt động không nhận đơn mới, phân công mới, điều chỉnh kho hay chuyển kho mới; nhân viên tạm dừng thao tác nghiệp vụ. Admin vẫn xem dữ liệu, đối soát đơn đã hoàn hàng và kích hoạt lại chi nhánh. Checkout và tạo phiếu khóa chi nhánh để tránh phát sinh công việc mới đồng thời với việc ngừng hoạt động.

Lịch sử mới được ghi cùng transaction của thao tác. Phiếu/tài khoản có sẵn từ trước có thể chưa có đầy đủ sự kiện cũ; hệ thống không dựng lại thời điểm hoặc người thao tác không biết. Hai lịch sử bổ sung không thay đổi quan hệ User → Role và User → Branch hiện có.

Các kiểm thử vận hành nằm ở `OperationsWorkspaceIntegrationTest`, gồm đăng nhập session thật, khóa tài khoản, đổi vai trò/chi nhánh, truy cập sai phạm vi, chi nhánh ngừng hoạt động, giao diện các tab và duyệt/nhận hàng đồng thời. Bộ test dùng H2; cần kiểm tra triển khai SQL Server và bố cục trình duyệt trong môi trường chạy thực.
