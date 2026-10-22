package vn.iotstar.dto.cart;

public record UpdateCartItemDto(
        @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(99) int quantity) {
}
