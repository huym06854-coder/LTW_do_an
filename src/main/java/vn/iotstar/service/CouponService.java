package vn.iotstar.service;

import java.math.*;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.Coupon;
import vn.iotstar.domain.enums.DiscountType;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.repository.CouponRepository;

@Service
public class CouponService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final CouponRepository repository;

    public CouponService(CouponRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Coupon reserve(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank())
            return null;
        Coupon coupon = repository.lockByCode(code.trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new BusinessException("Mã ưu đãi không hợp lệ"));
        entityManager.refresh(coupon, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        validate(coupon, subtotal);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        return coupon;
    }

    @Transactional(readOnly = true)
    public Coupon preview(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank())
            return null;
        Coupon coupon = repository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Mã ưu đãi không hợp lệ"));
        validate(coupon, subtotal);
        return coupon;
    }

    private void validate(Coupon coupon, BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isActive() || now.isBefore(coupon.getStartsAt()) || now.isAfter(coupon.getExpiresAt()))
            throw new BusinessException("Mã ưu đãi không còn hiệu lực");
        if (coupon.getMinimumOrderAmount() != null && subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0)
            throw new BusinessException("Đơn hàng chưa đủ điều kiện sử dụng mã");
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit())
            throw new BusinessException("Mã ưu đãi đã hết lượt dùng");
    }

    public BigDecimal discount(Coupon coupon, BigDecimal subtotal) {
        if (coupon == null)
            return BigDecimal.ZERO;
        BigDecimal result = coupon.getDiscountType() == DiscountType.FIXED_AMOUNT ? coupon.getDiscountValue()
                : subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (coupon.getMaximumDiscountAmount() != null)
            result = result.min(coupon.getMaximumDiscountAmount());
        return result.min(subtotal).max(BigDecimal.ZERO);
    }

    @Transactional
    public void restore(Coupon coupon) {
        if (coupon != null) {
            Coupon locked = repository.lockByCode(coupon.getCode()).orElseThrow();
            entityManager.refresh(locked, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (locked.getUsedCount() > 0)
                locked.setUsedCount(locked.getUsedCount() - 1);
        }
    }
}
