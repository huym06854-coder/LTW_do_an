package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.BranchStock;
import vn.iotstar.domain.enums.*;

public interface BranchStockRepository extends JpaRepository<BranchStock, Long> {
    List<BranchStock> findByBranchId(Long branchId);

    Optional<BranchStock> findByBranchIdAndProductId(Long branchId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BranchStock s where s.branch.id = :branchId and s.product.id = :productId")
    Optional<BranchStock> lockByBranchAndProduct(@Param("branchId") Long branchId, @Param("productId") Long productId);
}
