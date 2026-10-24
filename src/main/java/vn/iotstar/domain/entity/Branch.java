package vn.iotstar.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import vn.iotstar.domain.enums.BranchStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "branches", uniqueConstraints = @UniqueConstraint(name = "uk_branches_code", columnNames = "code"))
public class Branch extends BaseEntity {

	@Column(nullable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 160)
	@Nationalized
	private String name;

	@Column(nullable = false, length = 500)
	@Nationalized
	private String address;

	@Column(length = 30)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BranchStatus status = BranchStatus.ACTIVE;

	@OneToMany(mappedBy = "branch")
	private List<User> staff = new ArrayList<>();
}
