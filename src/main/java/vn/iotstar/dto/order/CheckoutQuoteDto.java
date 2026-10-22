package vn.iotstar.dto.order;

import java.math.BigDecimal;

public record CheckoutQuoteDto(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discountAmount,
        BigDecimal totalAmount) {
}
