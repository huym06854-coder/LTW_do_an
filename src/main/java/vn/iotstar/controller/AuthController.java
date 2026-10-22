package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.auth.RegisterDto;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.service.UserService;

@Controller
public class AuthController {
    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login() {
        return "store/login";
    }

    @GetMapping("/register")
    public String form() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String username,
            @RequestParam String email, @RequestParam String password, @RequestParam(required = false) String phone,
            RedirectAttributes flash) {
        try {
            users.register(new RegisterDto(fullName, username, email, password, phone));
            flash.addFlashAttribute("message", "Đăng ký thành công, vui lòng đăng nhập");
            return "redirect:/login";
        } catch (BusinessException e) {
            return registrationFailure(e.getMessage(), flash);
        } catch (DataIntegrityViolationException e) {
            return registrationFailure(
                    "Email hoặc tên đăng nhập đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng thông tin khác.", flash);
        }
    }

    private String registrationFailure(String message, RedirectAttributes flash) {
        flash.addFlashAttribute("registrationError", message);
        return "redirect:/register";
    }
}
