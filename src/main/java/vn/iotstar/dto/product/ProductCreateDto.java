package vn.iotstar.dto.product;

public record ProductCreateDto(@jakarta.validation.constraints.NotBlank String sku,
        @jakarta.validation.constraints.NotBlank String name,
        String description,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.0") java.math.BigDecimal price,
        @jakarta.validation.constraints.NotNull Long categoryId, Long brandId) {
}
