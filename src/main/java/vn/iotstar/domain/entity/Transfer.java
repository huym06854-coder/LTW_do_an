package vn.iotstar.domain.entity;

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
import vn.iotstar.domain.enums.TransferStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transfers", uniqueConstraints = @UniqueConstraint(name = "uk_transfers_code", columnNames = "code"))
public class Transfer extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_branch_id", nullable = false)
	private Branch sourceBranch;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_branch_id", nullable = false)
	private Branch destinationBranch;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransferStatus status = TransferStatus.DRAFT;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by")
	private User approvedBy;

	@Column(length = 1000)
	private String note;

	@OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TransferItem> items = new ArrayList<>();
}
