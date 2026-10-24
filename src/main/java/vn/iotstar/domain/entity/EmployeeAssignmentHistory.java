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
@Table(name = "employee_assignment_history")
public class EmployeeAssignmentHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by")
    private User changedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "previous_branch_id")
    private Branch previousBranch;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "new_branch_id")
    private Branch newBranch;
    @Enumerated(EnumType.STRING) @Column(name = "previous_role", length = 20)
    private UserRole previousRole;
    @Enumerated(EnumType.STRING) @Column(name = "new_role", nullable = false, length = 20)
    private UserRole newRole;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 20)
    private UserStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 20)
    private UserStatus newStatus;
    @Nationalized @Column(length = 500)
    private String note;
}

