package vn.iotstar.dto.order;

public record OrderDto(Long id, String orderNumber, String status, java.math.BigDecimal totalAmount,
        Long branchId, java.util.List<OrderItemDto> items) {
    public String getStatusLabel() {
        return vn.iotstar.domain.enums.OrderStatus.valueOf(status).getLabel();
    }
}
