package vn.iotstar.controller;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.order.*;
import vn.iotstar.exception.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

@Controller
public class CheckoutController {
    private final OrderService orders;
    private final CartService carts;
    private final AddressService addresses;
    private final UserService users;
    private final ShippingFeeService shipping;

    public CheckoutController(OrderService orders, CartService carts, AddressService addresses, UserService users,
            ShippingFeeService shipping) {
        this.orders = orders;
        this.carts = carts;
        this.addresses = addresses;
        this.users = users;
        this.shipping = shipping;
    }

    @GetMapping("/checkout")
    public String page(@AuthenticationPrincipal CustomerUserDetails principal, Model model, RedirectAttributes flash) {
        var cart = carts.view(principal.getId());
        if (cart.items().isEmpty() || cart.branchId() == null) {
            flash.addFlashAttribute("cartError", "Thêm sản phẩm và chọn chi nhánh trong giỏ hàng trước khi đặt hàng.");
            return "redirect:/cart";
        }
        var user = users.profileById(principal.getId());
        Long defaultId = addresses.list(user.getId()).stream().filter(a -> a.isDefault()).map(a -> a.id()).findFirst()
                .orElse(null);
        var form = new CheckoutDto(cart.branchId(), user.getFullName(), user.getPhone(), null,
                DeliveryMethod.SHIP_TO_ADDRESS,
                PaymentMethod.CASH_ON_DELIVERY, null, null, defaultId, false, null, null, null, null,
                UUID.randomUUID().toString());
        model.addAttribute("checkout", form);
        prepare(principal.getId(), form, model);
        return "cart/checkout";
    }

    @PostMapping("/checkout/place-order")
    public String place(@AuthenticationPrincipal CustomerUserDetails principal,
            @Valid @ModelAttribute("checkout") CheckoutDto dto, BindingResult errors, Model model) {
        validateForm(dto, errors);
        if (!errors.hasErrors()) {
            try {
                return "redirect:/orders/" + orders.checkout(principal.getId(), dto).id();
            } catch (CheckoutValidationException error) {
                errors.rejectValue(error.getField(), "invalid", error.getMessage());
            } catch (ResourceNotFoundException error) {
                errors.rejectValue("addressId", "invalid", "Địa chỉ giao hàng không hợp lệ. Vui lòng chọn lại.");
            } catch (BusinessException error) {
                errors.reject("checkout", error.getMessage());
            }
        }
        prepare(principal.getId(), dto, model);
        return "cart/checkout";
    }

    @PostMapping("/checkout/quote")
    @ResponseBody
    public ResponseEntity<?> quote(@AuthenticationPrincipal CustomerUserDetails principal,
            @ModelAttribute CheckoutDto dto) {
        try {
            return ResponseEntity.ok(orders.quote(principal.getId(), dto));
        } catch (BusinessException error) {
            return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
        }
    }

    private void validateForm(CheckoutDto form, BindingResult errors) {
        boolean manual = form.deliveryMethod() == DeliveryMethod.PICKUP_AT_BRANCH || form.addressId() == null;
        if (manual) {
            required(errors, "recipientName", form.recipientName(), "Nhập họ tên người nhận");
            if (form.recipientPhone() == null || !form.recipientPhone().matches("[+0-9][0-9 .()-]{7,29}"))
                errors.rejectValue("recipientPhone", "phone", "Số điện thoại phải có từ 8 đến 30 ký tự hợp lệ");
        }
        if (form.deliveryMethod() == DeliveryMethod.SHIP_TO_ADDRESS && form.addressId() == null) {
            required(errors, "province", form.province(), "Nhập tỉnh/thành phố");
            required(errors, "district", form.district(), "Nhập quận/huyện");
            required(errors, "ward", form.ward(), "Nhập phường/xã");
            required(errors, "addressDetail", form.addressDetail(), "Nhập số nhà, tên đường");
        }
    }

    private void required(BindingResult errors, String field, String value, String message) {
        if (value == null || value.isBlank())
            errors.rejectValue(field, "required", message);
    }

    private void prepare(Long userId, CheckoutDto form, Model model) {
        var cart = carts.view(userId);
        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addresses.list(userId));
        try {
            model.addAttribute("quote", orders.quote(userId, form));
        } catch (BusinessException error) {
            var fee = shipping.calculate(form.deliveryMethod(), cart.subtotal());
            model.addAttribute("quote",
                    new CheckoutQuoteDto(cart.subtotal(), fee, java.math.BigDecimal.ZERO, cart.subtotal().add(fee)));
            model.addAttribute("quoteError", error.getMessage());
        }
    }
}
