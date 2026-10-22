package vn.iotstar.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Vui lòng nhập tên người nhận") @Size(max = 160, message = "Tên người nhận tối đa 160 ký tự") String recipientName,
        @NotBlank(message = "Vui lòng nhập số điện thoại") @Pattern(regexp = "^[+0-9][0-9 .()-]{7,29}$", message = "Số điện thoại phải có từ 8 đến 30 ký tự hợp lệ") String phone,
        @NotBlank(message = "Vui lòng nhập tỉnh/thành phố") @Size(max = 120, message = "Tỉnh/thành phố tối đa 120 ký tự") String province,
        @NotBlank(message = "Vui lòng nhập quận/huyện") @Size(max = 120, message = "Quận/huyện tối đa 120 ký tự") String district,
        @NotBlank(message = "Vui lòng nhập phường/xã") @Size(max = 120, message = "Phường/xã tối đa 120 ký tự") String ward,
        @NotBlank(message = "Vui lòng nhập số nhà, tên đường") @Size(max = 500, message = "Địa chỉ cụ thể tối đa 500 ký tự") String addressDetail,
        boolean defaultAddress) {

    public static AddressRequest empty() {
        return new AddressRequest("", "", "", "", "", "", false);
    }

    public static AddressRequest from(AddressDto address) {
        return new AddressRequest(address.recipientName(), address.phone(), address.province(),
                address.district(), address.ward(), address.addressDetail(), address.isDefault());
    }
}
