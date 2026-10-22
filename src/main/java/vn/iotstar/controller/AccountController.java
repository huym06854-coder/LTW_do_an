package vn.iotstar.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.domain.enums.OrderStatus;
import vn.iotstar.domain.enums.UserRole;
import vn.iotstar.dto.address.AddressRequest;
import vn.iotstar.service.AddressService;
import vn.iotstar.service.OrderService;
import vn.iotstar.service.UserService;

@Controller
public class AccountController {
    private final UserService users;
    private final OrderService orders;
    private final AddressService addresses;

    public AccountController(UserService users, OrderService orders, AddressService addresses) {
        this.users = users;
        this.orders = orders;
        this.addresses = addresses;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomerUserDetails principal, Model model,
            @RequestParam(defaultValue = "orders") String section,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long editAddressId) {
        var user = users.profileById(principal.getId());
        String activeSection = switch (section) {
            case "info", "addresses" -> section;
            default -> "orders";
        };
        String activeStatus = normalizeStatus(status);
        LocalDate rangeEnd = to == null ? LocalDate.now() : to;
        LocalDate rangeStart = from == null ? rangeEnd.minusYears(1) : from;
        if (rangeStart.isAfter(rangeEnd)) {
            LocalDate swap = rangeStart;
            rangeStart = rangeEnd;
            rangeEnd = swap;
            model.addAttribute("filterMessage", "Ngày bắt đầu và ngày kết thúc đã được đổi lại cho đúng thứ tự.");
        }

        model.addAttribute("profile", user);
        model.addAttribute("profileRole", roleLabel(user.getRole().getName()));
        model.addAttribute("profileStatus", switch (user.getStatus()) {
            case ACTIVE -> "Đang hoạt động";
            case INACTIVE -> "Chưa kích hoạt";
            case LOCKED -> "Đã khóa";
        });
        model.addAttribute("addresses", addresses.list(user.getId()));
        model.addAttribute("editAddressId", editAddressId);
        if (!model.containsAttribute("addressForm")) {
            model.addAttribute("addressForm", editAddressId == null ? AddressRequest.empty()
                    : AddressRequest.from(addresses.getOwned(user.getId(), editAddressId)));
        } else if (editAddressId != null) {
            addresses.getOwned(user.getId(), editAddressId);
        }
        model.addAttribute("activeSection", activeSection);
        model.addAttribute("activeStatus", activeStatus);
        model.addAttribute("fromDate", rangeStart);
        model.addAttribute("toDate", rangeEnd);
        model.addAttribute("orders", activeSection.equals("orders")
                ? orders.customerOrderSummaries(user.getId(), activeStatus, rangeStart.atStartOfDay(),
                        rangeEnd.plusDays(1).atStartOfDay(), PageRequest.of(Math.max(0, page), 10))
                : org.springframework.data.domain.Page.empty(PageRequest.of(0, 10)));
        model.addAttribute("orderStatusLabels", Arrays.stream(OrderStatus.values())
                .collect(Collectors.toMap(Function.identity(), OrderStatus::getLabel)));
        return "account/profile";
    }

    private String normalizeStatus(String status) {
        return switch (status) {
            case "pending", "shipping", "done", "cancelled" -> status;
            default -> "all";
        };
    }

    private String roleLabel(UserRole role) {
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case MANAGER -> "Quản lý";
            case STAFF -> "Nhân viên";
            case CUSTOMER -> "Khách hàng";
        };
    }
}
