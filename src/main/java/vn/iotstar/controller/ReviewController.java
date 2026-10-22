package vn.iotstar.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.ReviewService;

@Controller
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping("/products/{id}/reviews")
    public String submit(@PathVariable Long id,
            @RequestParam int rating, @RequestParam(required = false) String comment,
            @AuthenticationPrincipal CustomerUserDetails user) {
        reviews.submit(user.getId(), id, rating, comment);
        return "redirect:/product-detail?id=" + id;
    }
}
