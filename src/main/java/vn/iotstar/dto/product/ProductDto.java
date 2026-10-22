package vn.iotstar.dto.product;

public record ProductDto(Long id, String sku, String name, String description, java.math.BigDecimal price,
        String category, String brand, String status, String imageUrl) {
}
