/** Chi tiết từng sản phẩm và số lượng trong một phiếu chuyển kho */
package vn.iotstar.domain.entity;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transfer_items",
		uniqueConstraints = @UniqueConstraint(name = "uk_transfer_item_product", columnNames = {
				"transfer_id", "product_id" }),
		check = @CheckConstraint(name = "ck_transfer_item_quantity", constraint = "quantity > 0"))
public class TransferItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transfer_id", nullable = false)
	private Transfer transfer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private int quantity;
}
