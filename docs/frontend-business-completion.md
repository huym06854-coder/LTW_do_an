# Hoàn thiện frontend và luồng mua hàng TechStore

## Đã triển khai

- Sổ địa chỉ thêm/sửa/xóa/đặt mặc định; kiểm tra chủ sở hữu và khóa theo tài khoản. Địa chỉ được chụp lại vào đơn hàng khi checkout.
- Checkout hai cột, chọn địa chỉ đã lưu hoặc nhập địa chỉ có cấu trúc, nhận tại cửa hàng/giao tận nơi, lưu địa chỉ mới, giữ dữ liệu và hiển thị lỗi tại trường nhập.
- Backend tính lại tạm tính, ưu đãi và phí giao hàng; có endpoint báo tổng tiền. Phí giao hàng cấu hình qua `SHIPPING_FLAT_FEE` và `SHIPPING_FREE_FROM`, mặc định miễn phí, không sử dụng số tiền do trình duyệt gửi lên.
- Mã phiên checkout kết hợp khóa database và unique index chống tạo đơn lặp. Kho được khóa theo thứ tự sản phẩm và tải lại sau khi chờ khóa để không sử dụng tồn kho cũ.
- Giỏ hàng dùng ảnh sản phẩm, tên/địa chỉ chi nhánh, tồn từng sản phẩm, nút xóa và cập nhật AJAX có debounce. Nút checkout chỉ mở khi chi nhánh và số lượng hợp lệ, không còn yêu cầu chưa lưu.
- Lọc/sắp xếp/phân trang sản phẩm trực tiếp ở database, bỏ giới hạn lọc trong 100 sản phẩm đầu. Trang chủ không còn gán rating/tồn kho giả và có tải lại khi API lỗi.
- Danh sách và chi tiết đơn khách/nhân viên/admin có bộ lọc, phân trang, thông tin giao nhận, tiền hàng/phí/giảm giá, thanh toán, thời điểm thu tiền và timeline.
- Nhận tại quầy đi qua `READY_FOR_PICKUP`. COD chỉ chuyển `PAID` khi nhân viên xác nhận đã thu đủ tiền. Lưu người xác nhận và thời điểm thu tiền.
- Giao thất bại đi qua `DELIVERY_FAILED`, giao lại hoặc `RETURNING`. Chỉ `RETURNED` mới hoàn tồn kho; thao tác lặp bị từ chối. Đơn đã thu tiền có xác nhận hoàn tiền thủ công riêng của quản lý/admin sau khi nhận hàng hoàn.
- Nhân viên chỉ xem/xử lý đơn của chi nhánh; khách chỉ xem đơn của mình. Giao diện chỉ hiện bước xử lý hợp lệ, backend kiểm tra lại mọi bước.
- Thông báo đơn mới gửi đến nhân viên/quản lý chi nhánh và admin qua hàng đợi WebSocket riêng sau khi transaction thành công. Chỉ cho phép tài khoản đã đăng nhập đăng ký kênh thông báo cá nhân; chặn nghe kênh của người khác, wildcard và gửi thông báo từ trình duyệt.
- Thống nhất tên TechStore và các trang mua hàng dùng stylesheet tùy biến; bỏ thông báo thành công giả ở yêu thích, nhận tin và liên hệ chưa được hỗ trợ.

## Kiểm thử và giới hạn

Đã chạy toàn bộ 49 bài kiểm thử ngày 22/10/2026: 0 thất bại, 0 lỗi, 0 bỏ qua; Maven báo `BUILD SUCCESS`. Các test tích hợp dùng H2, bao phủ quyền địa chỉ, một địa chỉ mặc định khi ghi đồng thời, snapshot địa chỉ, form checkout, chống checkout trùng/chống bán vượt tồn, COD, trạng thái nhận tại quầy, giao thất bại/hoàn kho/hoàn tiền, quyền xem đơn, phân trang trên 100 sản phẩm và render các template. Có kiểm tra riêng tình huống tồn kho cũ trong bộ nhớ sau giao dịch mua đồng thời và quyền truy cập kênh WebSocket.

Kiểm tra cú pháp JavaScript đã thành công với `site.js`, `cart.js`, `checkout.js` và `order-notifications.js`.

Maven có thể chạy với thư mục build riêng khi IDE đang biên dịch: `./mvnw.cmd -Dtechstore.build.directory=target-codex-verification test`.

Chưa xác nhận end-to-end trên SQL Server thực hoặc trình duyệt desktop/điện thoại; cần kiểm tra trực tiếp giao diện và nhận thông báo WebSocket trong môi trường chạy thật. Các media query đã được rà soát để loại giới hạn chiều rộng gây tràn giỏ hàng, checkout và bộ lọc ngày của hồ sơ.

## Khi khởi động lại

Flyway áp dụng SQL Server `V6__order_fulfillment.sql` / H2 `V4__order_fulfillment.sql` để thêm checkout token và thông tin đối soát thanh toán. Không sửa các migration cũ đã áp dụng. Điền biểu phí thực tế trong cấu hình nếu không miễn phí. Chức năng hoàn tiền chỉ ghi nhận tiền đã được nhân viên có quyền thực hiện, không tự chuyển tiền qua ngân hàng.

Khi gửi ZIP mã nguồn, không kèm `.env`, `.git`, thư mục build hoặc credential thật. Thay credential tại dịch vụ nếu đã chia sẻ trước đó.
