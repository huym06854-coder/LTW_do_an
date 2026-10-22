package vn.iotstar.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.service.CategoryService;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService categories;

    public AdminCategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categories.all());
        return "admin/categories";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam(required = false) String description) {
        categories.create(name, description);
        return "redirect:/admin/categories";
    }
}
