package vn.iotstar.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.domain.enums.ProductStatus;
import vn.iotstar.dto.product.*;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;

@Service
public class ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;

    public ProductService(ProductRepository products, CategoryRepository categories, BrandRepository brands) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> adminList() {
        return products.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> catalog(String query, Pageable pageable) {
        return catalog(query, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> catalog(String query, String category, String brand, String price, Pageable pageable) {
        Specification<Product> filters = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), ProductStatus.ACTIVE));
            if (query != null && !query.isBlank()) {
                String pattern = containsPattern(query);
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern, '\\'),
                        builder.like(builder.lower(root.get("sku")), pattern, '\\'),
                        builder.like(builder.lower(root.join("brand", JoinType.LEFT).get("name")), pattern, '\\')));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(builder.like(builder.lower(root.join("category").get("name")), containsPattern(category),
                        '\\'));
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.join("brand").get("name")),
                        brand.trim().toLowerCase(Locale.ROOT)));
            }
            if (price != null) {
                switch (price) {
                    case "under10" -> predicates.add(builder.lessThan(root.get("price"), new BigDecimal("10000000")));
                    case "10to20" -> predicates.add(
                            builder.between(root.get("price"), new BigDecimal("10000000"), new BigDecimal("20000000")));
                    case "over20" -> predicates.add(builder.greaterThan(root.get("price"), new BigDecimal("20000000")));
                    default -> {
                    }
                }
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Pageable sortedPage = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), catalogSort("newest"));
        return products.findAll(filters, sortedPage).map(this::toDto);
    }

    public static Sort catalogSort(String key) {
        return switch (key == null ? "" : key) {
            case "price-asc" -> Sort.by("price").ascending().and(Sort.by("id"));
            case "price-desc" -> Sort.by("price").descending().and(Sort.by("id"));
            default -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
        };
    }

    private static String containsPattern(String text) {
        return "%" + text.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
                + "%";
    }

    @Transactional(readOnly = true)
    public List<String> categoryNames() {
        return categories.findAllByOrderByNameAsc().stream().map(Category::getName).toList();
    }

    @Transactional(readOnly = true)
    public List<String> brandNames() {
        return brands.findAllByOrderByNameAsc().stream().map(Brand::getName).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto detail(Long id) {
        return toDto(findActive(id));
    }

    @Transactional(readOnly = true)
    public Product findActive(Long id) {
        var p = products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        if (p.getStatus() != ProductStatus.ACTIVE)
            throw new ResourceNotFoundException("Sản phẩm không còn kinh doanh");
        return p;
    }

    @Transactional(readOnly = true)
    public ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                p.getCategory().getName(), p.getBrand() == null ? null : p.getBrand().getName(), p.getStatus().name(),
                p.getImages().stream().filter(img -> img.isPrimary()).findFirst().map(ProductImage::getImageUrl)
                        .orElse(null));
    }

    @Transactional
    public ProductDto create(ProductCreateDto request) {
        if (request.sku() == null || request.sku().isBlank() || request.name() == null || request.name().isBlank()
                || request.price() == null)
            throw new BusinessException("Thông tin sản phẩm không hợp lệ");
        if (request.price().compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("Giá không hợp lệ");
        if (products.findBySku(request.sku()).isPresent())
            throw new BusinessException("SKU đã tồn tại");
        Product p = new Product();
        p.setSku(request.sku().trim());
        p.setName(request.name().trim());
        p.setDescription(request.description());
        p.setPrice(request.price());
        p.setCategory(categories.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục")));
        if (request.brandId() != null)
            p.setBrand(brands.findById(request.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu")));
        return toDto(products.save(p));
    }

    @Transactional
    public ProductDto update(Long id, ProductUpdateDto dto) {
        Product p = products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        if (dto.name() == null || dto.name().isBlank() || dto.price() == null || dto.price().signum() < 0)
            throw new BusinessException("Tên hoặc giá sản phẩm không hợp lệ");
        p.setName(dto.name().trim());
        p.setDescription(dto.description());
        p.setPrice(dto.price());
        if (dto.categoryId() != null)
            p.setCategory(categories.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại")));
        if (dto.brandId() != null)
            p.setBrand(brands.findById(dto.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại")));
        return toDto(p);
    }

    @Transactional
    public void setStatus(Long productId, ProductStatus status) {
        Product p = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        p.setStatus(status);
    }
}
