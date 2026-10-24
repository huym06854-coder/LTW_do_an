package vn.iotstar.dto.order;

import jakarta.validation.constraints.*;
import vn.iotstar.domain.enums.*;

public record CheckoutDto(
        @NotNull(message = "Vui lòng chọn chi nhánh") Long branchId,
        @Size(max = 160, message = "Tên người nhận tối đa 160 ký tự") String recipientName,
        @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự") String recipientPhone,
        @Size(max = 1000) String shippingAddress,
        @NotNull(message = "Vui lòng chọn hình thức nhận hàng") DeliveryMethod deliveryMethod,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 40) String couponCode, @Size(max = 1000) String note,
        Long addressId, Boolean saveAddress,
        @Size(max = 120) String province, @Size(max = 120) String district,
        @Size(max = 120) String ward, @Size(max = 500) String addressDetail,
        @Pattern(regexp = "[A-Za-z0-9-]{16,64}", message = "Phiên đặt hàng không hợp lệ") String checkoutToken,
        @Positive Long buyNowProductId, @Min(1) @Max(99) Integer buyNowQuantity) {
    public CheckoutDto(Long branchId, String recipientName, String recipientPhone, String shippingAddress,
            DeliveryMethod deliveryMethod, PaymentMethod paymentMethod, String couponCode, String note) {
        this(branchId, recipientName, recipientPhone, shippingAddress, deliveryMethod, paymentMethod, couponCode, note,
                null, false, null, null, null, null, null, null, null);
    }

    public CheckoutDto(Long branchId, String recipientName, String recipientPhone, String shippingAddress,
            DeliveryMethod deliveryMethod, PaymentMethod paymentMethod, String couponCode, String note,
            Long addressId, boolean saveAddress, String province, String district, String ward,
            String addressDetail, String checkoutToken) {
        this(branchId, recipientName, recipientPhone, shippingAddress, deliveryMethod, paymentMethod,
                couponCode, note, addressId, saveAddress, province, district, ward, addressDetail,
                checkoutToken, null, null);
    }
}
