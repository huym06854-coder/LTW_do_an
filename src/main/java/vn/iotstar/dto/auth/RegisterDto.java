package vn.iotstar.dto.auth;

public record RegisterDto(@jakarta.validation.constraints.NotBlank String fullName,
        @jakarta.validation.constraints.NotBlank String username,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email String email,
        @jakarta.validation.constraints.Size(min = 8, max = 72) String password, String phone) {
}
