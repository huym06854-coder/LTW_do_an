package vn.iotstar.dto.cart;

public record AddToCartDto(@jakarta.validation.constraints.NotNull Long productId,
        @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(99) int quantity) {
}
