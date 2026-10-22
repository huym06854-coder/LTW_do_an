package vn.iotstar.controller.admin;

import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.domain.enums.ProductStatus;
import vn.iotstar.dto.product.ProductCreateDto;
import vn.iotstar.service.ProductService;
import vn.iotstar.service.ProductImageService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductService products;
    private final ProductImageService images;

    public AdminProductController(ProductService products, ProductImageService images) {
        this.products = products;
        this.images = images;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("products", products.adminList());
        return "admin/products";
    }

    @PostMapping
    public String create(@RequestParam String sku, @RequestParam String name,
            @RequestParam BigDecimal price, @RequestParam Long categoryId, @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String description) {
        products.create(new ProductCreateDto(sku, name, description, price, categoryId, brandId));
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam String name,
            @RequestParam BigDecimal price, @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long brandId) {
        products.update(id, new vn.iotstar.dto.product.ProductUpdateDto(name, description, price, categoryId, brandId));
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/image")
    public String upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        images.replacePrimary(id, file);
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam ProductStatus status) {
        products.setStatus(id, status);
        return "redirect:/admin/products";
    }
}
