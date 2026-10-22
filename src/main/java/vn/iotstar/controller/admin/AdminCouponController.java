package vn.iotstar.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.domain.entity.Coupon;
import vn.iotstar.domain.enums.DiscountType;
import vn.iotstar.repository.CouponRepository;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {
    private final CouponRepository coupons;

    public AdminCouponController(CouponRepository coupons) {
        this.coupons = coupons;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("coupons", coupons.findAll());
        return "admin/coupons";
    }

    @PostMapping
    public String create(@RequestParam String code, @RequestParam BigDecimal discountValue,
            @RequestParam DiscountType discountType, @RequestParam String expiresAt) {
        if (discountValue.signum() <= 0)
            throw new IllegalArgumentException("Giảm giá phải lớn hơn 0");
        Coupon c = new Coupon();
        c.setCode(code.trim().toUpperCase());
        c.setDiscountType(discountType);
        c.setDiscountValue(discountValue);
        c.setStartsAt(LocalDateTime.now());
        c.setExpiresAt(LocalDateTime.parse(expiresAt));
        c.setActive(true);
        coupons.save(c);
        return "redirect:/admin/coupons";
    }
}
