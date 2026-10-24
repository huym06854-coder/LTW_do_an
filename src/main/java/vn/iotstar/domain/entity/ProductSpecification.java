/** Lưu cấu hình RAM, CPU, SSD, kích thước màn hình… */
package vn.iotstar.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_specifications", uniqueConstraints = @UniqueConstraint(name = "uk_product_specification", columnNames = {
		"product_id", "spec_name" }))
public class ProductSpecification extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "spec_name", nullable = false, length = 120)
	@Nationalized
	private String specName;

	@Column(name = "spec_value", nullable = false, length = 500)
	@Nationalized
	private String specValue;
}
