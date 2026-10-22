package vn.iotstar.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "image_url", nullable = false, length = 1000)
	private String imageUrl;

	@Column(name = "cloudinary_public_id", length = 255)
	private String cloudinaryPublicId;

	@Column(name = "is_primary", nullable = false)
	private boolean isPrimary;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;
}
