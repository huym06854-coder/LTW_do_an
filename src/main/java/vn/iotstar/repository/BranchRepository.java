package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.Branch;
import vn.iotstar.domain.enums.*;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByStatusOrderByNameAsc(BranchStatus status);
}
