package vn.iotstar.dto.stock;

public record StockAdjustmentDto(@jakarta.validation.constraints.NotNull Long branchId,
        @jakarta.validation.constraints.NotNull Long productId, int quantityChange,
        @jakarta.validation.constraints.NotBlank String reason) {
}
