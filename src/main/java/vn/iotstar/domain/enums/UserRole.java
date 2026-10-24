package vn.iotstar.domain.enums;

public enum UserRole {
	ADMIN,
	MANAGER,
	STAFF,
	CUSTOMER;

    public String getLabel() {
        return switch (this) {
            case ADMIN -> "Quản trị viên";
            case MANAGER -> "Quản lý chi nhánh";
            case STAFF -> "Nhân viên";
            case CUSTOMER -> "Khách hàng";
        };
    }
}
