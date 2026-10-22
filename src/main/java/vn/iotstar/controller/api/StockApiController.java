package vn.iotstar.controller.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.*;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.UserRole;
import vn.iotstar.dto.stock.*;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

@RestController
@RequestMapping("/api/stocks")
public class StockApiController {
    private final InventoryService inventory;
    private final UserService users;

    public StockApiController(InventoryService inventory, UserService users) {
        this.inventory = inventory;
        this.users = users;
    }

    @GetMapping("/branch/{branchId}")
    public List<StockDto> atBranch(@AuthenticationPrincipal CustomerUserDetails principal,
            @PathVariable Long branchId) {
        checkScope(users.byId(principal.getId()), branchId);
        return inventory.atBranch(branchId);
    }

    @PostMapping("/adjustments")
    public void adjust(@AuthenticationPrincipal CustomerUserDetails principal,
            @Valid @RequestBody StockAdjustmentDto dto) {
        User actor = users.byId(principal.getId());
        checkScope(actor, dto.branchId());
        inventory.adminAdjust(dto.branchId(), dto.productId(), dto.quantityChange(), dto.reason(), actor);
    }

    private void checkScope(User actor, Long branchId) {
        if (actor.getRole().getName() != UserRole.ADMIN
                && (actor.getBranch() == null || !actor.getBranch().getId().equals(branchId)))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không được thao tác trên chi nhánh khác");
    }
}
