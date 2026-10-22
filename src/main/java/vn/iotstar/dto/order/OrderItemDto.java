package vn.iotstar.dto.order;

public record OrderItemDto(Long productId, String productName, int quantity, java.math.BigDecimal unitPrice,
        java.math.BigDecimal subtotal) {
}
