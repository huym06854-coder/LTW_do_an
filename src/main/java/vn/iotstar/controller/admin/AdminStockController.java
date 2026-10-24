package vn.iotstar.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.exception.BusinessException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/stock")
public class AdminStockController {
    private final InventoryService inventory;
    private final UserService users;
    private final BranchService branches;
    private final ProductRepository products;

    public AdminStockController(InventoryService inventory, UserService users, BranchService branches, ProductRepository products) {
        this.inventory = inventory;
        this.users = users;
        this.branches = branches;
        this.products = products;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long branchId, Model model) {
        var active = branches.active();
        if (branchId == null && !active.isEmpty()) branchId = active.getFirst().getId();
        model.addAttribute("branches", active);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("stocks", branchId == null ? java.util.List.of() : inventory.rowsAtBranch(branchId));
        model.addAttribute("productOptions", products.findActiveOptions());
        return "admin/stock";
    }

    @PostMapping("/update")
    public String adjust(@AuthenticationPrincipal CustomerUserDetails actor,
            @RequestParam Long branchId, @RequestParam Long productId, @RequestParam int quantityChange,
            @RequestParam String reason, RedirectAttributes redirect) {
        try {
            inventory.adminAdjust(branchId, productId, quantityChange, reason, users.byId(actor.getId()));
            redirect.addFlashAttribute("success", "Đã cập nhật tồn kho và ghi nhận lý do điều chỉnh.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/stock?branchId=" + branchId;
    }
}
