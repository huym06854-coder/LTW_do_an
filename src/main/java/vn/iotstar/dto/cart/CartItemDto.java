package vn.iotstar.dto.cart;
public record CartItemDto(Long productId, String name, int quantity, java.math.BigDecimal price,
        java.math.BigDecimal subtotal, String imageUrl, Integer availableStock) {
    public CartItemDto(Long productId, String name, int quantity, java.math.BigDecimal price, java.math.BigDecimal subtotal) {
        this(productId, name, quantity, price, subtotal, null, null);
    }
}
