package vn.iotstar.controller.staff;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.UserService;

@ControllerAdvice(basePackageClasses = StaffDashboardController.class)
public class StaffWorkspaceAdvice {
    private final UserService users;
    public StaffWorkspaceAdvice(UserService users) { this.users = users; }

    @ModelAttribute
    public void workspace(@AuthenticationPrincipal CustomerUserDetails principal, Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        if (principal == null) return;
        var actor = users.profileById(principal.getId());
        if (actor.getStatus() != vn.iotstar.domain.enums.UserStatus.ACTIVE)
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã ngừng hoạt động");
        model.addAttribute("staffBranch", actor.getBranch());
        boolean paused = actor.getBranch() != null && actor.getBranch().getStatus() != vn.iotstar.domain.enums.BranchStatus.ACTIVE;
        model.addAttribute("branchPaused", paused);
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (paused && !java.util.Set.of("/staff", "/staff/", "/staff/dashboard").contains(path))
            throw new org.springframework.security.access.AccessDeniedException("Chi nhánh đã ngừng hoạt động");
        model.addAttribute("staffRole", actor.getRole().getName().name());
    }
}
