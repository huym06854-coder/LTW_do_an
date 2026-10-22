package vn.iotstar.controller;

import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.OrderService;

@Controller
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/orders")
    public String list(@AuthenticationPrincipal CustomerUserDetails user, Model model,
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
        model.addAttribute("orders",
                orders.myOrders(user.getId(), selected, from, to, PageRequest.of(Math.max(0, page), 10)));
        model.addAttribute("listPath", "/orders");
        model.addAttribute("pageHeading", "Đơn hàng của tôi");
        model.addAttribute("pageDescription", "Theo dõi việc xử lý, giao nhận và thanh toán của từng đơn hàng.");
        return "orders/list";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails user, Model model) {
        model.addAttribute("order", orders.myOrderDetail(user.getId(), id));
        OrderPageSupport.detail(model, "/orders", false);
        return "orders/detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails user,
            RedirectAttributes redirect) {
        try {
            orders.cancelByCustomer(user.getId(), id);
            redirect.addFlashAttribute("orderSuccess", "Đã hủy đơn hàng và hoàn lại số lượng sản phẩm.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("orderError", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
