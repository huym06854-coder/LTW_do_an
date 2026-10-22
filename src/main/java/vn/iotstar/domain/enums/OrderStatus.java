package vn.iotstar.domain.enums;

public enum OrderStatus {
	PENDING("Chờ xác nhận"),
	CONFIRMED("Đã xác nhận"),
	PREPARING("Đang chuẩn bị"),
	READY_FOR_PICKUP("Sẵn sàng nhận hàng"),
	SHIPPING("Đang giao"),
	DELIVERY_FAILED("Giao không thành công"),
	RETURNING("Đang hoàn hàng"),
	RETURNED("Đã nhận hàng hoàn"),
	COMPLETED("Hoàn tất"),
	CANCELLED("Đã hủy");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
