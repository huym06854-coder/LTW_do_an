/** Cho khách hàng lưu nhiều địa chỉ giao hàng */
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
@Table(name = "user_addresses")
public class UserAddress extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "recipient_name", nullable = false, length = 160)
	private String recipientName;

	@Column(nullable = false, length = 30)
	private String phone;

	@Column(nullable = false, length = 120)
	private String province;

	@Column(nullable = false, length = 120)
	private String district;

	@Column(nullable = false, length = 120)
	private String ward;

	@Column(name = "address_detail", nullable = false, length = 500)
	private String addressDetail;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;
}
