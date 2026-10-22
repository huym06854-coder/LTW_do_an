package vn.iotstar.dto.auth;

public record LoginDto(@jakarta.validation.constraints.NotBlank String login,
        @jakarta.validation.constraints.NotBlank String password) {
}
