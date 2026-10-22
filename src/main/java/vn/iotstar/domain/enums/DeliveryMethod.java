package vn.iotstar.domain.enums;

public enum DeliveryMethod {
	SHIP_TO_ADDRESS,
	PICKUP_AT_BRANCH;
    public String getLabel() { return this == PICKUP_AT_BRANCH ? "Nhận tại cửa hàng" : "Giao tận nơi"; }
}
