package vn.iotstar.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.iotstar.domain.enums.StockMovementType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_logs")
public class StockLog extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "quantity_change", nullable = false)
	private int quantityChange;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private StockMovementType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "performed_by")
	private User performedBy;

	@Column(name = "reference_code", length = 80)
	private String referenceCode;

	@Column(length = 1000)
	private String note;
}
