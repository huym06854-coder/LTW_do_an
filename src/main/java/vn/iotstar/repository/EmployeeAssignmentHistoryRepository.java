package vn.iotstar.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import vn.iotstar.domain.entity.*;

public interface EmployeeAssignmentHistoryRepository extends JpaRepository<EmployeeAssignmentHistory, Long> {
    @EntityGraph(attributePaths = {"changedBy", "previousBranch", "newBranch"})
    List<EmployeeAssignmentHistory> findTop50ByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}

