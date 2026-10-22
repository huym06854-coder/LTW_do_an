package vn.iotstar.domain.enums;

public enum PaymentStatus {
	PENDING,
	PAID,
	FAILED,
	REFUNDED;
    public String getLabel() { return switch(this) { case PENDING -> "Chưa thanh toán"; case PAID -> "Đã thu tiền"; case FAILED -> "Thanh toán thất bại"; case REFUNDED -> "Đã hoàn tiền"; }; }
}
