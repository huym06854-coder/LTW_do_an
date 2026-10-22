package vn.iotstar.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.NotificationService;

@Controller
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/notifications")
    public String list(@AuthenticationPrincipal CustomerUserDetails user, Model model) {
        model.addAttribute("notifications", notifications.forUser(user.getId(), PageRequest.of(0, 50)).getContent());
        return "account/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String mark(@PathVariable Long id, @AuthenticationPrincipal CustomerUserDetails user) {
        notifications.markRead(id, user.getId());
        return "redirect:/notifications";
    }
}
