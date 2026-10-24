package vn.iotstar.dto.stock;

public record StockRowDto(Long branchId, Long productId, String productName, String sku, int quantity) {}
