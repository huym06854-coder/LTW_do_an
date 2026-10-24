package vn.iotstar.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import vn.iotstar.domain.enums.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transfer_status_history")
public class TransferStatusHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 20)
    private TransferStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 20)
    private TransferStatus newStatus;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by")
    private User changedBy;
    @Nationalized @Column(length = 500)
    private String note;
}

