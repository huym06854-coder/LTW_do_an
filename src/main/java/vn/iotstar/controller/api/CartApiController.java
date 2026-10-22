package vn.iotstar.controller.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.dto.cart.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {
    private final CartService service;

    public CartApiController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartDto view(@AuthenticationPrincipal CustomerUserDetails user) {
        return service.view(user.getId());
    }

    @PostMapping("/items")
    public CartDto add(@AuthenticationPrincipal CustomerUserDetails user, @Valid @RequestBody AddToCartDto request) {
        service.add(user.getId(), request.productId(), request.quantity());
        return service.view(user.getId());
    }

    @PatchMapping("/items/{productId}")
    public CartDto update(@AuthenticationPrincipal CustomerUserDetails user,
            @PathVariable Long productId, @Valid @RequestBody UpdateCartItemDto request) {
        service.update(user.getId(), productId, request.quantity());
        return service.view(user.getId());
    }

    @PutMapping("/branch/{branchId}")
    public CartDto chooseBranch(@AuthenticationPrincipal CustomerUserDetails user, @PathVariable Long branchId) {
        service.chooseBranch(user.getId(), branchId);
        return service.view(user.getId());
    }
}
