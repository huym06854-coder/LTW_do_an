package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.Transfer;
import vn.iotstar.domain.enums.*;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findBySourceBranchIdOrDestinationBranchIdOrderByCreatedAtDesc(Long source, Long destination);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transfer t where t.id = :id")
    Optional<Transfer> lockById(@Param("id") Long id);
}
