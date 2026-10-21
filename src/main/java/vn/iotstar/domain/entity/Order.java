package vn.iotstar.domain.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.iotstar.domain.enums.DeliveryMethod;
import vn.iotstar.domain.enums.OrderStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(name = "uk_orders_order_number", columnNames = "order_number"))
public class Order extends BaseEntity {

	@Column(name = "order_number", nullable = false, length = 40)
	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coupon_id")
	private Coupon coupon;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_method", nullable = false, length = 30)
	private DeliveryMethod deliveryMethod;

	@Column(name = "recipient_name", nullable = false, length = 160)
	private String recipientName;

	@Column(name = "recipient_phone", nullable = false, length = 30)
	private String recipientPhone;

	@Column(name = "shipping_address", length = 1000)
	private String shippingAddress;

	@Column(precision = 19, scale = 2, nullable = false)
	private BigDecimal subtotal;

	@Column(name = "shipping_fee", precision = 19, scale = 2, nullable = false)
	private BigDecimal shippingFee;

	@Column(name = "discount_amount", precision = 19, scale = 2, nullable = false)
	private BigDecimal discountAmount;

	@Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
	private BigDecimal totalAmount;

	@Column(length = 1000)
	private String note;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderStatusHistory> statusHistory = new ArrayList<>();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Payment> payments = new ArrayList<>();
}
