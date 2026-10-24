package vn.iotstar.controller.admin;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.domain.enums.*;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

@Controller
@RequestMapping("/admin/branches")
public class AdminBranchController {
    private final BranchService branches;
    private final UserRepository users;
    private final UserService accounts;
    private final OrderService orders;
    private final InventoryService inventory;
    private final TransferService transfers;
    private final ReportService reports;

    public AdminBranchController(BranchService branches, UserRepository users, UserService accounts,
            OrderService orders, InventoryService inventory, TransferService transfers, ReportService reports) {
        this.branches = branches; this.users = users; this.accounts = accounts; this.orders = orders;
        this.inventory = inventory; this.transfers = transfers; this.reports = reports;
    }

    @GetMapping
    public String list(Model model) {
        var summaries = branches.summaries();
        model.addAttribute("branches", summaries);
        model.addAttribute("activeBranchCount", summaries.stream().filter(b -> b.status() == BranchStatus.ACTIVE).count());
        model.addAttribute("assignedEmployeeCount", summaries.stream().mapToLong(b -> b.managerCount() + b.staffCount()).sum());
        model.addAttribute("pendingTransferCount", branches.pendingTransferCount());
        return "admin/branches";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue = "overview") String tab,
            @RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal CustomerUserDetails actor, Model model) {
        if (!java.util.Set.of("overview", "employees", "orders", "stock", "transfers").contains(tab)) tab = "overview";
        model.addAttribute("branch", branches.get(id));
        model.addAttribute("summary", branches.summaries().stream().filter(b -> b.id().equals(id)).findFirst().orElseThrow());
        model.addAttribute("activeTab", tab);
        var pageable = PageRequest.of(Math.max(0, page), 15, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        switch (tab) {
            case "employees" -> model.addAttribute("members", users.findByBranchIdAndRoleNameIn(id,
                    List.of(UserRole.STAFF, UserRole.MANAGER), pageable));
            case "orders" -> model.addAttribute("branchOrders", orders.adminOrdersAtBranch(id, accounts.profileById(actor.getId()), pageable));
            case "stock" -> model.addAttribute("stocks", inventory.rowsAtBranch(id));
            case "transfers" -> model.addAttribute("transfers", transfers.inBranch(id));
            default -> model.addAttribute("recentRevenue", reports.revenueByCompletedDate(id,
                    LocalDate.now().minusDays(29).atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay()));
        }
        return "admin/branch-detail";
    }

    @PostMapping
    public String create(@RequestParam String code, @RequestParam String name, @RequestParam String address, RedirectAttributes redirect) {
        try {
            branches.create(code, name, address);
            redirect.addFlashAttribute("success", "Đã thêm chi nhánh. Bạn có thể phân công nhân sự tại trang Người dùng.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            redirect.addFlashAttribute("branchDraft", java.util.Map.of("code", code, "name", name, "address", address));
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @RequestParam String name, @RequestParam String address, RedirectAttributes redirect) {
        try {
            branches.updateDetails(id, name, address);
            redirect.addFlashAttribute("success", "Đã lưu thông tin chi nhánh.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/branches";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam BranchStatus status, RedirectAttributes redirect) {
        try {
            branches.changeStatus(id, status);
            redirect.addFlashAttribute("success", "Đã cập nhật trạng thái chi nhánh.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/branches/" + id;
    }
}
