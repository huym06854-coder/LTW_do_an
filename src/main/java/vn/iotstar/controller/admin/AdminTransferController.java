package vn.iotstar.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import vn.iotstar.repository.TransferRepository;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.exception.BusinessException;

@Controller
@RequestMapping("/admin/transfers")
public class AdminTransferController {
    private final TransferService transfers;
    private final UserService users;
    private final TransferRepository repository;

    public AdminTransferController(TransferService transfers, UserService users, TransferRepository repository) {
        this.transfers = transfers;
        this.users = users;
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("transfers", repository.findAllByOrderByCreatedAtDesc());
        return "admin/transfers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails actor, Model model) {
        var detail = transfers.detail(id, users.profileById(actor.getId()));
        model.addAttribute("detail", detail);
        model.addAttribute("transferRows", java.util.List.of(detail.transfer()));
        model.addAttribute("staffMode", false);
        return "admin/transfer-detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails actor, RedirectAttributes redirect) {
        try {
            transfers.approve(id, users.byId(actor.getId()));
            redirect.addFlashAttribute("success", "Đã duyệt phiếu và trừ kho nguồn.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/transfers";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails actor, RedirectAttributes redirect) {
        try {
            transfers.cancel(id, users.byId(actor.getId()));
            redirect.addFlashAttribute("success", "Đã hủy phiếu chuyển kho.");
        } catch (BusinessException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/transfers";
    }
}
