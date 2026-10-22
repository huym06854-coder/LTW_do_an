package vn.iotstar.dto.cart;

public record CartDto(Long branchId, java.util.List<CartItemDto> items, java.math.BigDecimal subtotal,
        String branchName, String branchAddress) {
    public CartDto(Long branchId, java.util.List<CartItemDto> items, java.math.BigDecimal subtotal) {
        this(branchId, items, subtotal, null, null);
    }

    public boolean isCheckoutAllowed() {
        return branchId != null && !items.isEmpty() && items.stream()
                .allMatch(item -> item.availableStock() != null && item.quantity() <= item.availableStock());
    }
}
