package vn.iotstar.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import vn.iotstar.domain.enums.DiscountType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "coupons", uniqueConstraints = @UniqueConstraint(name = "uk_coupons_code", columnNames = "code"))
public class Coupon extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@Column(length = 500)
	@Nationalized
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 20)
	private DiscountType discountType;

	@Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "minimum_order_amount", precision = 19, scale = 2)
	private BigDecimal minimumOrderAmount;

	@Column(name = "maximum_discount_amount", precision = 19, scale = 2)
	private BigDecimal maximumDiscountAmount;

	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "usage_limit")
	private Integer usageLimit;

	@Column(name = "used_count", nullable = false)
	private int usedCount;

	@Column(nullable = false)
	private boolean active = true;
}
