package vn.iotstar.dto.order;

import java.math.BigDecimal;

public record OrderDetailItemDto(Long productId, String productName, String sku, String imageUrl,
                int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
