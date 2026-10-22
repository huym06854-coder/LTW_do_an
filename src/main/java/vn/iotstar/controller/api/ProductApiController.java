package vn.iotstar.controller.api;

import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.dto.product.*;
import vn.iotstar.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {
    private final ProductService service;

    public ProductApiController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ProductDto> catalog(@RequestParam(required = false) String q,
            @RequestParam(required = false) String category, @RequestParam(required = false) String brand,
            @RequestParam(required = false) String price, @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.catalog(q, category, brand, price,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), ProductService.catalogSort(sort)));
    }

    @GetMapping("/{id}")
    public ProductDto detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductCreateDto request) {
        return service.create(request);
    }
}
