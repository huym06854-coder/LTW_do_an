package vn.iotstar.controller.staff;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;
import vn.iotstar.exception.BusinessException;

@Controller
@RequestMapping("/staff/stock")
public class StaffStockController {
    private final InventoryService inventory;
    private final UserService users;
    public StaffStockController(InventoryService inventory, UserService users) { this.inventory = inventory; this.users = users; }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomerUserDetails principal, Model model) {
        var actor = users.branchOperator(principal.getId());
        var rows = inventory.rowsAtBranch(actor.getBranch().getId());
        model.addAttribute("stocks", rows);
        model.addAttribute("lowStockCount", rows.stream().filter(s -> s.quantity() > 0 && s.quantity() <= 5).count());
        model.addAttribute("outOfStockCount", rows.stream().filter(s -> s.quantity() == 0).count());
        return "staff/stock";
    }

    @PostMapping("/update")
    public String adjust(@AuthenticationPrincipal CustomerUserDetails principal, @RequestParam Long productId,
            @RequestParam int quantityChange, @RequestParam String reason, RedirectAttributes redirect) {
        var actor = users.branchOperator(principal.getId());
        try {
            inventory.adminAdjust(actor.getBranch().getId(), productId, quantityChange, reason, actor);
            redirect.addFlashAttribute("success", "Đã cập nhật tồn kho và lưu lý do điều chỉnh.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/staff/stock";
    }
}
