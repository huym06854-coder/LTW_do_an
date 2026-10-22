package vn.iotstar.service;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.iotstar.domain.enums.DeliveryMethod;

@Service
public class ShippingFeeService {
    private final BigDecimal flatFee;
    private final BigDecimal freeFrom;

    public ShippingFeeService(@Value("${app.shipping.flat-fee:0}") BigDecimal flatFee,
            @Value("${app.shipping.free-from:0}") BigDecimal freeFrom) {
        if (flatFee.signum() < 0 || freeFrom.signum() < 0)
            throw new IllegalArgumentException("Phí vận chuyển không được âm");
        this.flatFee = flatFee;
        this.freeFrom = freeFrom;
    }

    public BigDecimal calculate(DeliveryMethod delivery, BigDecimal subtotal) {
        if (delivery == DeliveryMethod.PICKUP_AT_BRANCH || freeFrom.signum() > 0 && subtotal.compareTo(freeFrom) >= 0)
            return BigDecimal.ZERO;
        return flatFee;
    }
}
