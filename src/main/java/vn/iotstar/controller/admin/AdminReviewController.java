package vn.iotstar.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.repository.ReviewRepository;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    private final ReviewRepository reviews;

    public AdminReviewController(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("reviews", reviews.findAll());
        return "admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        reviews.deleteById(id);
        return "redirect:/admin/reviews";
    }
}
