package vn.iotstar.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.domain.entity.Brand;
import vn.iotstar.repository.BrandRepository;

@Controller
@RequestMapping("/admin/brands")
public class AdminBrandController {
    private final BrandRepository brands;

    public AdminBrandController(BrandRepository brands) {
        this.brands = brands;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("brands", brands.findAllByOrderByNameAsc());
        return "admin/brands";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam(required = false) String description) {
        Brand b = new Brand();
        b.setName(name.trim());
        b.setDescription(description);
        brands.save(b);
        return "redirect:/admin/brands";
    }
}
