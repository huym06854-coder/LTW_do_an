package vn.iotstar.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import vn.iotstar.repository.TransferRepository;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

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
        model.addAttribute("transfers", repository.findAll());
        return "admin/transfers";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails actor) {
        transfers.approve(id, users.byId(actor.getId()));
        return "redirect:/admin";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails actor) {
        transfers.cancel(id, users.byId(actor.getId()));
        return "redirect:/admin";
    }
}
