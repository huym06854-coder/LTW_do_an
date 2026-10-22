package vn.iotstar.controller.staff;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;
import vn.iotstar.exception.BusinessException;
@Controller
@RequestMapping("/staff/transfers")
public class StaffTransferController {
    private final TransferService transfers;private final UserService users;private final BranchService branches;
    public StaffTransferController(TransferService transfers,UserService users,BranchService branches){
        this.transfers=transfers;this.users=users;this.branches=branches;
    }
    @GetMapping public String list(@AuthenticationPrincipal CustomerUserDetails actor,Model model){
        var user=users.byId(actor.getId());
        if(user.getBranch()==null)throw new BusinessException("Chưa được phân công chi nhánh");
        model.addAttribute("transfers",transfers.inBranch(user.getBranch().getId()));
        model.addAttribute("branches",branches.active());return "staff/transfers";
    }
    @PostMapping public String create(@AuthenticationPrincipal CustomerUserDetails actor,
           @RequestParam Long destinationBranchId,@RequestParam Long productId,@RequestParam int quantity){
        var user=users.byId(actor.getId());
        if(user.getBranch()==null)throw new BusinessException("Chưa được phân công chi nhánh");
        transfers.create(user.getBranch().getId(),destinationBranchId,Map.of(productId,quantity),user);
        return "redirect:/staff/transfers";
    }
    @PostMapping("/{id}/ship")public String ship(@PathVariable Long id,@AuthenticationPrincipal CustomerUserDetails actor){
        transfers.ship(id,users.byId(actor.getId()));return "redirect:/staff/transfers";
    }
    @PostMapping("/{id}/complete")public String complete(@PathVariable Long id,@AuthenticationPrincipal CustomerUserDetails actor){
        transfers.complete(id,users.byId(actor.getId()));return "redirect:/staff/transfers";
    }
}
