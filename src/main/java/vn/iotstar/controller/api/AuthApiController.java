package vn.iotstar.controller.api;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.dto.auth.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.security.jwt.JwtService;
import vn.iotstar.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthenticationManager auth;
    private final JwtService jwt;
    private final UserService users;

    public AuthApiController(AuthenticationManager auth, JwtService jwt, UserService users) {
        this.auth = auth;
        this.jwt = jwt;
        this.users = users;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginDto dto) {
        var result = auth.authenticate(new UsernamePasswordAuthenticationToken(dto.login(), dto.password()));
        return new LoginResponseDto(jwt.issue(result.getName()), "Bearer");
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterDto dto) {
        var user = users.register(dto);
        return Map.of("id", user.getId(), "username", user.getUsername());
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal CustomerUserDetails principal) {
        var user = users.byId(principal.getId());
        return Map.of("id", user.getId(), "username", user.getUsername(), "role", user.getRole().getName());
    }
}
