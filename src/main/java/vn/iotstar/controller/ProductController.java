package vn.iotstar.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.service.ProductService;

@Controller
public class ProductController {
    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q,
            @RequestParam(required = false) String category, @RequestParam(required = false) String brand,
            @RequestParam(required = false) String price, @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page, Model model) {
        if (!java.util.Set.of("newest", "price-asc", "price-desc").contains(sort))
            sort = "newest";
        var catalog = products.catalog(q, category, brand, price,
                PageRequest.of(Math.max(0, page), 12, ProductService.catalogSort(sort)));
        model.addAttribute("products", catalog.getContent());
        model.addAttribute("catalogPage", catalog);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("brand", brand);
        model.addAttribute("price", price);
        model.addAttribute("sort", sort);
        model.addAttribute("categoryOptions", products.categoryNames());
        model.addAttribute("brandOptions", products.brandNames());
        model.addAttribute("firstPage", Math.max(0, catalog.getNumber() - 2));
        model.addAttribute("lastPage", Math.min(catalog.getTotalPages() - 1, catalog.getNumber() + 2));
        return "store/products";
    }

    @GetMapping("/product-detail")
    public String detail(@RequestParam Long id, Model model) {
        model.addAttribute("product", products.detail(id));
        return "store/product-detail";
    }
}
