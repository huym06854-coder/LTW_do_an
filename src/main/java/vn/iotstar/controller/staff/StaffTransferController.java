package vn.iotstar.controller.staff;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;
import vn.iotstar.exception.BusinessException;

@Controller
@RequestMapping("/staff/transfers")
public class StaffTransferController {
    private final TransferService transfers;
    private final UserService users;
    private final BranchService branches;
    private final InventoryService inventory;
    public StaffTransferController(TransferService transfers, UserService users, BranchService branches, InventoryService inventory) {
        this.transfers = transfers; this.users = users; this.branches = branches; this.inventory = inventory;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomerUserDetails principal, Model model) {
        var actor = users.branchOperator(principal.getId());
        Long branchId = actor.getBranch().getId();
        model.addAttribute("transfers", transfers.inBranch(branchId));
        model.addAttribute("branches", branches.active().stream().filter(b -> !b.getId().equals(branchId)).toList());
        model.addAttribute("stockOptions", inventory.rowsAtBranch(branchId).stream().filter(s -> s.quantity() > 0).toList());
        return "staff/transfers";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CustomerUserDetails principal, @RequestParam Long destinationBranchId,
            @RequestParam Long productId, @RequestParam int quantity, RedirectAttributes redirect) {
        var actor = users.branchOperator(principal.getId());
        try {
            transfers.create(actor.getBranch().getId(), destinationBranchId, Map.of(productId, quantity), actor);
            redirect.addFlashAttribute("success", "Đã gửi yêu cầu chuyển kho. Hàng sẽ được trừ khi phiếu được duyệt.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/staff/transfers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails principal, Model model) {
        var detail = transfers.detail(id, users.branchOperator(principal.getId()));
        model.addAttribute("detail", detail);
        model.addAttribute("transferRows", java.util.List.of(detail.transfer()));
        model.addAttribute("staffMode", true);
        return "staff/transfer-detail";
    }

    @PostMapping("/{id}/{action:approve|cancel|ship|complete}")
    public String action(@PathVariable Long id, @PathVariable String action,
            @AuthenticationPrincipal CustomerUserDetails principal, RedirectAttributes redirect) {
        var actor = users.branchOperator(principal.getId());
        try {
            switch (action) {
                case "approve" -> transfers.approve(id, actor);
                case "cancel" -> transfers.cancel(id, actor);
                case "ship" -> transfers.ship(id, actor);
                case "complete" -> transfers.complete(id, actor);
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
            redirect.addFlashAttribute("success", "Đã cập nhật phiếu chuyển kho.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/staff/transfers";
    }
}
