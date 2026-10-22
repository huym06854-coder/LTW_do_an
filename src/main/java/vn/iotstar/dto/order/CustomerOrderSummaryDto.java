package vn.iotstar.dto.order;

import java.math.BigDecimal;
import vn.iotstar.domain.enums.DeliveryMethod;
import vn.iotstar.domain.enums.OrderStatus;

public record CustomerOrderSummaryDto(
                Long id,
                String orderNumber,
                String createdAtLabel,
                OrderStatus status,
                String branchName,
                int itemCount,
                DeliveryMethod deliveryMethod,
                BigDecimal totalAmount) {
}
