package vn.iotstar.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import vn.iotstar.security.CustomerUserDetails;

@ControllerAdvice(annotations = Controller.class)
public class CurrentUserModelAdvice {

    @ModelAttribute
    public void addCurrentUser(Model model, @AuthenticationPrincipal CustomerUserDetails currentUser,
            HttpServletRequest request) {
        model.addAttribute("currentUser", currentUser);
        String activeNav = switch (request.getServletPath()) {
            case "/", "/index" -> "home";
            case "/products", "/product-detail" -> "products";
            case "/about" -> "about";
            case "/news" -> "news";
            case "/offers" -> "offers";
            case "/contact" -> "contact";
            default -> null;
        };
        model.addAttribute("activeNav", activeNav);
    }
}
