package vn.iotstar.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import vn.iotstar.security.CustomerUserDetails;

@ControllerAdvice(annotations = Controller.class)
public class CurrentUserModelAdvice {

    @ModelAttribute
    public void addCurrentUser(Model model, @AuthenticationPrincipal CustomerUserDetails currentUser) {
        model.addAttribute("currentUser", currentUser);
    }
}
