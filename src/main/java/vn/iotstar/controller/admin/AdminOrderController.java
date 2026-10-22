package vn.iotstar.controller.admin;

import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.controller.OrderPageSupport;
import vn.iotstar.domain.enums.OrderStatus;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.OrderService;
import vn.iotstar.service.UserService;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private final OrderService orders;
    private final UserService users;

    public AdminOrderController(OrderService orders, UserService users) {
        this.orders = orders;
        this.users = users;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomerUserDetails actor, Model model,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page) {
        if (from != null && to != null && from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
            model.addAttribute("filterMessage", "Đã đổi lại khoảng ngày để ngày bắt đầu đứng trước ngày kết thúc.");
        }
        var selected = OrderPageSupport.filters(model, status, from, to);
        model.addAttribute("orders", orders.managedOrders(users.profileById(actor.getId()), selected, from, to,
                PageRequest.of(Math.max(0, page), 10)));
        model.addAttribute("listPath", "/admin/orders");
        model.addAttribute("pageHeading", "Quản lý đơn hàng");
        model.addAttribute("pageDescription", "Theo dõi đơn hàng, giao nhận và thanh toán của toàn hệ thống.");
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails actor, Model model) {
        model.addAttribute("order", orders.branchOrderDetail(id, users.profileById(actor.getId())));
        OrderPageSupport.detail(model, "/admin/orders", true);
        return "admin/order-detail";
    }

    @PostMapping("/{id}/status")
    public String advance(@PathVariable Long id, @RequestParam OrderStatus status,
            @RequestParam(defaultValue = "false") boolean codCollected,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal CustomerUserDetails actor, RedirectAttributes redirect) {
        try {
            orders.advanceStatus(id, status, users.profileById(actor.getId()), codCollected, note);
            redirect.addFlashAttribute("orderSuccess", "Đã cập nhật trạng thái đơn hàng.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("orderError", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/refund")
    public String refund(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean confirmedRefund,
            @RequestParam(required = false) String note, @AuthenticationPrincipal CustomerUserDetails actor,
            RedirectAttributes redirect) {
        try {
            if (!confirmedRefund)
                throw new BusinessException("Vui lòng xác nhận đã thực hiện hoàn tiền cho khách hàng.");
            orders.confirmRefund(id, users.profileById(actor.getId()), note);
            redirect.addFlashAttribute("orderSuccess", "Đã ghi nhận hoàn tiền cho khách hàng.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("orderError", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
