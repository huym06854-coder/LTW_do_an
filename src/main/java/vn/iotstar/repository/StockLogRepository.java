package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.StockLog;
import vn.iotstar.domain.enums.*;

public interface StockLogRepository extends JpaRepository<StockLog, Long> {
    Page<StockLog> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);
}
