/**Lưu thông báo đơn hàng, tồn kho thấp, yêu cầu chuyển kho để hiển thị qua WebSocket */
package vn.iotstar.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 160)
	@Nationalized
	private String title;

	@Column(nullable = false, length = 2000)
	@Nationalized
	private String message;

	@Column(name = "notification_type", nullable = false, length = 40)
	private String type;

	@Column(name = "read_at")
	private LocalDateTime readAt;
}
