package vn.iotstar.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.domain.enums.UserStatus;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.AdminUserService;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserRepository repository;
    private final AdminUserService users;

    public AdminUserController(UserRepository repository, AdminUserService users) {
        this.repository = repository;
        this.users = users;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", repository.findAll());
        return "admin/users";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam UserStatus status,
            @AuthenticationPrincipal CustomerUserDetails actor) {
        users.changeStatus(id, actor.getId(), status);
        return "redirect:/admin/users";
    }
}
