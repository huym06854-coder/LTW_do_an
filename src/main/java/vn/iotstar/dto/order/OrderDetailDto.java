package vn.iotstar.dto.order;

import java.math.BigDecimal;
import java.util.List;
import vn.iotstar.domain.enums.*;

public record OrderDetailDto(Long id, String orderNumber, OrderStatus status, String createdAt, String completedAt,
        Long branchId, String branchName, String branchAddress, DeliveryMethod deliveryMethod,
        String recipientName, String recipientPhone, String shippingAddress, String note,
        BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discountAmount, BigDecimal totalAmount,
        List<OrderDetailItemDto> items, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
        List<OrderHistoryDto> histories, List<OrderStatus> allowedNextStatuses,
        boolean canCancel, boolean requiresCodCollection, boolean canConfirmRefund,
        String paidAt, String paymentConfirmedBy, String refundedAt) {
    public OrderDetailDto(Long id, String orderNumber, OrderStatus status, String createdAt, String completedAt,
            Long branchId, String branchName, String branchAddress, DeliveryMethod deliveryMethod,
            String recipientName, String recipientPhone, String shippingAddress, String note,
            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discountAmount, BigDecimal totalAmount,
            List<OrderDetailItemDto> items, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
            List<OrderHistoryDto> histories, List<OrderStatus> allowedNextStatuses,
            boolean canCancel, boolean requiresCodCollection, boolean canConfirmRefund) {
        this(id,orderNumber,status,createdAt,completedAt,branchId,branchName,branchAddress,deliveryMethod,
                recipientName,recipientPhone,shippingAddress,note,subtotal,shippingFee,discountAmount,totalAmount,
                items,paymentMethod,paymentStatus,histories,allowedNextStatuses,canCancel,requiresCodCollection,
                canConfirmRefund,null,null,null);
    }
    public String getStatusLabel() {
        return status.getLabel();
    }

    public String getDeliveryMethodLabel() {
        return deliveryMethod.getLabel();
    }

    public String getPaymentMethodLabel() {
        return paymentMethod == null ? "Chưa ghi nhận" : paymentMethod.getLabel();
    }

    public String getPaymentStatusLabel() {
        return paymentStatus == null ? "Chưa ghi nhận" : paymentStatus.getLabel();
    }
}
