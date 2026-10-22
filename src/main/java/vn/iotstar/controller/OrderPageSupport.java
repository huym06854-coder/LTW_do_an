package vn.iotstar.controller;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.ui.Model;
import vn.iotstar.domain.enums.OrderStatus;

/**
 * Shared labels and safe filter defaults for customer and operator order pages.
 */
public final class OrderPageSupport {
    private OrderPageSupport() {
    }

    public static OrderStatus filters(Model model, String status, LocalDate from, LocalDate to) {
        OrderStatus selected = null;
        if (status != null && !status.isBlank()) {
            try {
                selected = OrderStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                model.addAttribute("filterMessage", "Trạng thái không hợp lệ. Đang hiển thị tất cả đơn hàng.");
            }
        }
        model.addAttribute("activeStatus", selected == null ? "" : selected.name());
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        return selected;
    }

    public static void detail(Model model, String basePath, boolean manageOrder) {
        model.addAttribute("listPath", basePath);
        model.addAttribute("manageOrder", manageOrder);
        model.addAttribute("actionLabels", Map.ofEntries(
                Map.entry("CONFIRMED", "Xác nhận đơn hàng"),
                Map.entry("PREPARING", "Bắt đầu chuẩn bị"),
                Map.entry("READY_FOR_PICKUP", "Thông báo sẵn sàng nhận"),
                Map.entry("SHIPPING", "Bàn giao vận chuyển / giao lại"),
                Map.entry("COMPLETED", "Xác nhận bàn giao thành công"),
                Map.entry("DELIVERY_FAILED", "Ghi nhận giao thất bại"),
                Map.entry("RETURNING", "Tiếp nhận yêu cầu trả hàng"),
                Map.entry("RETURNED", "Xác nhận đã nhận hàng về kho"),
                Map.entry("CANCELLED", "Hủy đơn hàng")));
    }
}
