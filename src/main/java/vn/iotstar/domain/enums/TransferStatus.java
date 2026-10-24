package vn.iotstar.domain.enums;

public enum TransferStatus {
	DRAFT("Bản nháp"),
	PENDING("Chờ duyệt"),
	APPROVED("Đã duyệt"),
	IN_TRANSIT("Đang vận chuyển"),
	COMPLETED("Đã nhận hàng"),
	CANCELLED("Đã hủy");

    private final String label;

    TransferStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
