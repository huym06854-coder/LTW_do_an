package vn.iotstar.controller.staff;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.iotstar.domain.enums.*;
import vn.iotstar.repository.OrderRepository;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

@Controller
public class StaffDashboardController {
    private final UserService users;
    private final InventoryService inventory;
    private final OrderRepository orders;
    private final TransferService transfers;

    public StaffDashboardController(UserService users, InventoryService inventory, OrderRepository orders, TransferService transfers) {
        this.users = users; this.inventory = inventory; this.orders = orders; this.transfers = transfers;
    }

    @GetMapping({"/staff", "/staff/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomerUserDetails principal, Model model) {
        var actor = users.byId(principal.getId());
        if (actor.getRole().getName() == UserRole.ADMIN) return "redirect:/admin";
        if (actor.getBranch() == null) return "staff/index";
        if (actor.getBranch().getStatus() != BranchStatus.ACTIVE) return "staff/index";
        actor = users.branchOperator(principal.getId());
        Long branchId = actor.getBranch().getId();
        var stock = inventory.rowsAtBranch(branchId);
        model.addAttribute("pendingOrders", orders.countByBranchIdAndStatus(branchId, OrderStatus.PENDING));
        model.addAttribute("preparingOrders", orders.countByBranchIdAndStatus(branchId, OrderStatus.PREPARING));
        model.addAttribute("shippingOrders", orders.countByBranchIdAndStatus(branchId, OrderStatus.SHIPPING));
        model.addAttribute("lowStocks", stock.stream().filter(s -> s.quantity() <= 5).toList());
        var branchTransfers = transfers.inBranch(branchId);
        model.addAttribute("openTransferCount", branchTransfers.stream().filter(t -> t.getStatus() != TransferStatus.CANCELLED && t.getStatus() != TransferStatus.COMPLETED).count());
        model.addAttribute("recentTransfers", branchTransfers.stream().limit(5).toList());
        return "staff/index";
    }
}
