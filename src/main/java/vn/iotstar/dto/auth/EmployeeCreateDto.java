package vn.iotstar.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import vn.iotstar.domain.enums.UserRole;

@Getter
@Setter
public class EmployeeCreateDto {
    @NotBlank(message = "Nhập họ tên") @Size(max = 160)
    private String fullName;
    @NotBlank(message = "Nhập tên đăng nhập") @Size(max = 80)
    @Pattern(regexp = "[^@\\s]+", message = "Tên đăng nhập không chứa @ hoặc khoảng trắng")
    private String username;
    @NotBlank(message = "Nhập email") @Email(message = "Email không hợp lệ") @Size(max = 254)
    private String email;
    @NotBlank(message = "Nhập mật khẩu") @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
    private String password;
    @NotNull(message = "Chọn vai trò")
    private UserRole role = UserRole.STAFF;
    @NotNull(message = "Chọn chi nhánh")
    private Long branchId;
}
