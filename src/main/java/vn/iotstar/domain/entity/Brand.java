package vn.iotstar.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "brands")
public class Brand extends BaseEntity {

	@Column(nullable = false, unique = true, length = 120)
	@Nationalized
	private String name;

	@Column(length = 1000)
	@Nationalized
	private String description;

	@Column(name = "logo_url", length = 1000)
	private String logoUrl;
}
