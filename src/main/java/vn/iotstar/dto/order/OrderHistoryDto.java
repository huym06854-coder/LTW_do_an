package vn.iotstar.dto.order;

import vn.iotstar.domain.enums.OrderStatus;

public record OrderHistoryDto(OrderStatus previousStatus, OrderStatus newStatus, String createdAt,
                String changedByName, String note) {
    public String getLabel() {
        return previousStatus==OrderStatus.RETURNED&&newStatus==OrderStatus.RETURNED
                &&note!=null&&note.startsWith("Đã xác nhận hoàn tiền")?"Đã hoàn tiền":newStatus.getLabel();
    }
}
