package vn.iotstar.domain.enums;

public enum UserStatus {
	ACTIVE,
	INACTIVE,
	LOCKED;

    public String getLabel() {
        return switch (this) {
            case ACTIVE -> "Hoạt động";
            case INACTIVE -> "Ngừng hoạt động";
            case LOCKED -> "Đã khóa";
        };
    }
}
