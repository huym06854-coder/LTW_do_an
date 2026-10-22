package vn.iotstar.dto.product;

public record ProductUpdateDto(String name, String description, java.math.BigDecimal price, Long categoryId,
        Long brandId) {
}
