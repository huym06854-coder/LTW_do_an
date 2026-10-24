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
    private final BranchService branches;
    private final InventoryService inventory;

    public CheckoutController(OrderService orders, CartService carts, AddressService addresses, UserService users,
            ShippingFeeService shipping, BranchService branches, InventoryService inventory) {
        this.orders = orders;
        this.carts = carts;
        this.addresses = addresses;
        this.users = users;
        this.shipping = shipping;
        this.branches = branches;
        this.inventory = inventory;
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

    @PostMapping("/checkout/buy-now")
    public String buyNow(@RequestParam Long productId, @RequestParam int quantity, RedirectAttributes redirect) {
        try {
            carts.previewPurchase(productId, quantity, null);
        } catch (BusinessException | ResourceNotFoundException error) {
            redirect.addFlashAttribute("productError", error.getMessage());
            redirect.addFlashAttribute("requestedQuantity", quantity);
            return "redirect:/product-detail?id=" + productId;
        }
        redirect.addAttribute("productId", productId);
        redirect.addAttribute("quantity", quantity);
        return "redirect:/checkout/buy-now";
    }

    @GetMapping("/checkout/buy-now")
    public String buyNowPage(@AuthenticationPrincipal CustomerUserDetails principal,
            @RequestParam Long productId, @RequestParam int quantity, Model model, RedirectAttributes redirect) {
        try {
            carts.previewPurchase(productId, quantity, null);
            var selected = branches.active().stream()
                    .filter(branch -> inventory.available(branch.getId(), productId) >= quantity)
                    .findFirst().orElseThrow(() -> new BusinessException("Sản phẩm chưa đủ hàng tại các chi nhánh."));
            var cart = carts.previewPurchase(productId, quantity, selected.getId());
            var user = users.profileById(principal.getId());
            Long defaultId = addresses.list(user.getId()).stream().filter(a -> a.isDefault())
                    .map(a -> a.id()).findFirst().orElse(null);
            var form = new CheckoutDto(cart.branchId(), user.getFullName(), user.getPhone(), null,
                    DeliveryMethod.SHIP_TO_ADDRESS, PaymentMethod.CASH_ON_DELIVERY, null, null,
                    defaultId, false, null, null, null, null, UUID.randomUUID().toString(), productId, quantity);
            model.addAttribute("checkout", form);
            prepare(principal.getId(), form, model);
            return "cart/checkout";
        } catch (BusinessException | ResourceNotFoundException error) {
            redirect.addFlashAttribute("productError", error.getMessage());
            return "redirect:/product-detail?id=" + productId;
        }
    }

    @PostMapping("/checkout/place-order")
    public String place(@AuthenticationPrincipal CustomerUserDetails principal,
            @Valid @ModelAttribute("checkout") CheckoutDto dto, BindingResult errors, Model model) {
        if (dto.buyNowProductId() != null && (dto.buyNowProductId() <= 0 || dto.buyNowQuantity() == null
                || dto.buyNowQuantity() < 1 || dto.buyNowQuantity() > 99))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Thông tin sản phẩm đặt hàng không hợp lệ");
        validateForm(dto, errors);
        if (!errors.hasErrors()) {
            try {
                return "redirect:/orders/" + orders.checkout(principal.getId(), dto).id();
            } catch (CheckoutValidationException error) {
                errors.rejectValue(error.getField(), "invalid", error.getMessage());
            } catch (ResourceNotFoundException error) {
                if (dto.buyNowProductId() == null)
                    errors.rejectValue("addressId", "invalid", "Địa chỉ giao hàng không hợp lệ. Vui lòng chọn lại.");
                else
                    throw error;
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
        } catch (BusinessException | ResourceNotFoundException error) {
            return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
        }
    }

    @PostMapping("/checkout/branch")
    @ResponseBody
    public ResponseEntity<?> chooseBranch(@AuthenticationPrincipal CustomerUserDetails principal,
            @RequestParam Long branchId, @RequestParam(required = false) Long buyNowProductId,
            @RequestParam(required = false) Integer buyNowQuantity) {
        try {
            if (buyNowProductId != null) {
                if (buyNowQuantity == null)
                    throw new BusinessException("Số lượng không hợp lệ");
                var branchCart = carts.previewPurchase(buyNowProductId, buyNowQuantity, branchId);
                if (!branchCart.isCheckoutAllowed())
                    throw new BusinessException("Chi nhánh không đủ số lượng sản phẩm đã chọn");
                return ResponseEntity.ok(Map.of("branchId", branchCart.branchId(), "branchName", branchCart.branchName(),
                        "branchAddress", branchCart.branchAddress(), "checkoutAllowed", true));
            }
            carts.chooseBranch(principal.getId(), branchId);
            var cart = carts.view(principal.getId());
            return ResponseEntity.ok(Map.of("branchId", cart.branchId(), "branchName", cart.branchName(),
                    "branchAddress", cart.branchAddress(), "checkoutAllowed", cart.isCheckoutAllowed()));
        } catch (BusinessException | ResourceNotFoundException error) {
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
        var cart = form.buyNowProductId() == null ? carts.view(userId)
                : carts.previewPurchase(form.buyNowProductId(), form.buyNowQuantity(), form.branchId());
        model.addAttribute("cart", cart);
        model.addAttribute("buyNow", form.buyNowProductId() != null);
        model.addAttribute("addresses", addresses.list(userId));
        model.addAttribute("branchChoices", branches.active().stream().map(branch -> {
            var shortage = cart.items().stream()
                    .filter(item -> inventory.available(branch.getId(), item.productId()) < item.quantity())
                    .map(item -> item.name()).toList();
            return new BranchChoice(branch.getId(), branch.getName(), branch.getAddress(), shortage.isEmpty(),
                    shortage.isEmpty() ? "Đủ hàng" : "Thiếu: " + String.join(", ", shortage));
        }).toList());
        try {
            model.addAttribute("quote", orders.quote(userId, form));
        } catch (BusinessException error) {
            var fee = shipping.calculate(form.deliveryMethod(), cart.subtotal());
            model.addAttribute("quote",
                    new CheckoutQuoteDto(cart.subtotal(), fee, java.math.BigDecimal.ZERO, cart.subtotal().add(fee)));
            model.addAttribute("quoteError", error.getMessage());
        }
    }

    public record BranchChoice(Long id, String name, String address, boolean available, String stockLabel) {}
}
