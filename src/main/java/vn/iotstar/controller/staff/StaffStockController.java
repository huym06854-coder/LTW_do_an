package vn.iotstar.controller.staff;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;
import vn.iotstar.exception.BusinessException;
@Controller
@RequestMapping("/staff/stock")
public class StaffStockController {
    private final InventoryService inventory;private final UserService users;
    public StaffStockController(InventoryService inventory,UserService users){this.inventory=inventory;this.users=users;}
    @PostMapping("/update")public String adjust(@AuthenticationPrincipal CustomerUserDetails principal,
            @RequestParam Long productId,@RequestParam int quantityChange,@RequestParam String reason){
        var actor=users.byId(principal.getId());
        if(actor.getBranch()==null)throw new org.springframework.security.access.AccessDeniedException("Nhân viên chưa thuộc chi nhánh");
        inventory.adminAdjust(actor.getBranch().getId(),productId,quantityChange,reason,actor);
        return "redirect:/staff/orders";
    }
}
