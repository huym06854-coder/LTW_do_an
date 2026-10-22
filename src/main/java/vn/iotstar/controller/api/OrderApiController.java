package vn.iotstar.controller.api;

import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.dto.order.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
    private final OrderService orders;

    public OrderApiController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public Page<OrderDto> mine(@AuthenticationPrincipal CustomerUserDetails user,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return orders.myOrders(user.getId(), PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100))));
    }

    @GetMapping("/{id}")
    public OrderDetailDto detail(@AuthenticationPrincipal CustomerUserDetails user, @PathVariable Long id) {
        return orders.myOrderDetail(user.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@AuthenticationPrincipal CustomerUserDetails user, @Valid @RequestBody CheckoutDto dto) {
        return orders.checkout(user.getId(), dto);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@AuthenticationPrincipal CustomerUserDetails user, @PathVariable Long id) {
        orders.cancelByCustomer(user.getId(), id);
    }
}
