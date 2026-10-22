package vn.iotstar.domain.enums;

public enum PaymentMethod {
	CASH_ON_DELIVERY,
	BANK_TRANSFER,
	CARD;
    public String getLabel() { return switch(this) { case CASH_ON_DELIVERY -> "Thanh toán khi nhận hàng"; case BANK_TRANSFER -> "Chuyển khoản"; case CARD -> "Thẻ"; }; }
}
