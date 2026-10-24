package vn.iotstar.controller.admin;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.auth.EmployeeCreateDto;
import vn.iotstar.exception.*;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserRepository repository;
    private final AdminUserService users;
    private final BranchService branches;

    public AdminUserController(UserRepository repository, AdminUserService users, BranchService branches) {
        this.repository = repository; this.users = users; this.branches = branches;
    }

    @GetMapping
    public String list(Model model, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "employees") String group, @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) UserRole role, @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long branchId) {
        populate(model, group, q, role, status, branchId, page);
        model.addAttribute("employeeForm", new EmployeeCreateDto());
        return "admin/users";
    }

    private void populate(Model model, String group, String query, UserRole role, UserStatus status, Long branchId, int page) {
        if (!java.util.Set.of("employees", "customers", "all").contains(group)) group = "employees";
        var result = users.search(group, query, role, status, branchId, page);
        model.addAttribute("userPage", result); model.addAttribute("users", result.getContent());
        model.addAttribute("group", group); model.addAttribute("q", query);
        model.addAttribute("roleFilter", role); model.addAttribute("statusFilter", status);
        model.addAttribute("branchFilter", branchId); model.addAttribute("allBranches", branches.all());
        model.addAttribute("roles", UserRole.values()); model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("branches", branches.active());
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var account = repository.findWithRoleById(id).orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
        model.addAttribute("account", account);
        model.addAttribute("assignmentHistory", users.history(id));
        model.addAttribute("branches", branches.active());
        model.addAttribute("employeeRoles", java.util.List.of(UserRole.STAFF, UserRole.MANAGER));
        return "admin/user-detail";
    }

    @PostMapping("/employees")
    public String create(@Valid @ModelAttribute("employeeForm") EmployeeCreateDto form, BindingResult errors,
            @AuthenticationPrincipal CustomerUserDetails actor, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                users.createEmployee(form, actor.getId());
                redirect.addFlashAttribute("success", "Đã tạo tài khoản và phân công chi nhánh.");
                return "redirect:/admin/users";
            } catch (BusinessException ex) { errors.reject("employee", ex.getMessage()); }
            catch (org.springframework.dao.DataIntegrityViolationException ex) {
                errors.reject("employee", "Tên đăng nhập hoặc email đã được sử dụng.");
            }
        }
        form.setPassword(null);
        populate(model, "employees", "", null, null, null, 0);
        model.addAttribute("openEmployeeForm", true);
        return "admin/users";
    }

    @PostMapping("/{id}/assignment")
    public String assignment(@PathVariable Long id, @RequestParam Long branchId, @RequestParam UserRole role,
            @AuthenticationPrincipal CustomerUserDetails actor, RedirectAttributes redirect) {
        try {
            users.changeAssignment(id, branchId, role, actor.getId());
            redirect.addFlashAttribute("success", "Đã cập nhật vai trò / chi nhánh. Quyền mới được áp dụng ở lần truy cập tiếp theo.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/branch")
    public String assign(@PathVariable Long id, @RequestParam Long branchId,
            @AuthenticationPrincipal CustomerUserDetails actor, RedirectAttributes redirect) {
        try {
            users.assignBranch(id, branchId, actor.getId());
            redirect.addFlashAttribute("success", "Đã cập nhật chi nhánh của nhân sự.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam UserStatus status,
            @AuthenticationPrincipal CustomerUserDetails actor, RedirectAttributes redirect) {
        try {
            users.changeStatus(id, actor.getId(), status);
            redirect.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/users";
    }
}
