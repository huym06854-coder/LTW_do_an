package vn.iotstar.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.CartService;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.exception.ResourceNotFoundException;

@Controller
public class CartController {
    private final CartService carts;
    private final vn.iotstar.service.BranchService branches;

    public CartController(CartService carts, vn.iotstar.service.BranchService branches) {
        this.carts = carts;
        this.branches = branches;
    }

    @GetMapping("/cart/count")
    @ResponseBody
    public int count(@AuthenticationPrincipal CustomerUserDetails user) {
        return carts.view(user.getId()).items().stream().mapToInt(v -> v.quantity()).sum();
    }

    @GetMapping("/cart")
    public String page(@AuthenticationPrincipal CustomerUserDetails user, Model model) {
        model.addAttribute("cart", carts.view(user.getId()));
        model.addAttribute("branches", branches.active());
        return "store/cart";
    }

    @PostMapping("/cart/add")
    public String add(@AuthenticationPrincipal CustomerUserDetails user, @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity, RedirectAttributes redirect) {
        carts.add(user.getId(), productId, quantity);
        redirect.addFlashAttribute("message", "Đã thêm vào giỏ hàng");
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String update(@AuthenticationPrincipal CustomerUserDetails user,
            @RequestParam Long productId, @RequestParam int quantity, RedirectAttributes redirect) {
        try {
            carts.update(user.getId(), productId, quantity);
        } catch (BusinessException | ResourceNotFoundException error) {
            redirect.addFlashAttribute("cartError", error.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/quantity")
    @ResponseBody
    public ResponseEntity<?> quantity(@AuthenticationPrincipal CustomerUserDetails user,
            @RequestParam Long productId, @RequestParam int quantity) {
        try {
            carts.update(user.getId(), productId, quantity);
            return ResponseEntity.ok(carts.view(user.getId()));
        } catch (BusinessException | ResourceNotFoundException error) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", error.getMessage(), "cart", carts.view(user.getId())));
        }
    }

    @PostMapping("/cart/branch")
    public String branch(@AuthenticationPrincipal CustomerUserDetails user, @RequestParam Long branchId,
            RedirectAttributes redirect) {
        try {
            carts.chooseBranch(user.getId(), branchId);
            redirect.addFlashAttribute("message", "Đã chọn chi nhánh và kiểm tra tồn kho");
        } catch (BusinessException | ResourceNotFoundException error) {
            redirect.addFlashAttribute("cartError", error.getMessage());
        }
        return "redirect:/cart";
    }
}
