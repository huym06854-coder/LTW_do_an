package vn.iotstar.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

@Controller
@RequestMapping("/admin/stock")
public class AdminStockController {
    private final InventoryService inventory;
    private final UserService users;
    private final BranchService branches;

    public AdminStockController(InventoryService inventory, UserService users, BranchService branches) {
        this.inventory = inventory;
        this.users = users;
        this.branches = branches;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long branchId, Model model) {
        model.addAttribute("branches", branches.active());
        model.addAttribute("stocks", branchId == null ? java.util.List.of() : inventory.atBranch(branchId));
        return "admin/stock";
    }

    @PostMapping("/update")
    public String adjust(@AuthenticationPrincipal CustomerUserDetails actor,
            @RequestParam Long branchId, @RequestParam Long productId, @RequestParam int quantityChange,
            @RequestParam String reason) {
        inventory.adminAdjust(branchId, productId, quantityChange, reason, users.byId(actor.getId()));
        return "redirect:/admin/stock?branchId=" + branchId;
    }
}
