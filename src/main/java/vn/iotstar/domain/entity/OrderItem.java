package vn.iotstar.domain.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.CheckConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_items",
		uniqueConstraints = @UniqueConstraint(name = "uk_order_item_order_product", columnNames = {
				"order_id", "product_id" }),
		check = @CheckConstraint(name = "ck_order_item_quantity", constraint = "quantity > 0"))
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "product_name_snapshot", nullable = false, length = 200)
	@Nationalized
	private String productNameSnapshot;

	@Column(name = "sku_snapshot", nullable = false, length = 80)
	private String skuSnapshot;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal subtotal;
}
