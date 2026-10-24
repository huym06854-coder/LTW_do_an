package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.Transfer;
import vn.iotstar.domain.enums.*;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    long countByStatus(TransferStatus status);
    @Query("select t.sourceBranch.id, t.destinationBranch.id, count(t) from Transfer t where t.status = :status group by t.sourceBranch.id, t.destinationBranch.id")
    List<Object[]> countRoutesByStatus(@Param("status") TransferStatus status);

    @Query("select count(t) from Transfer t where (t.sourceBranch.id = :branchId or t.destinationBranch.id = :branchId) and t.status not in :terminal")
    long countOpenAtBranch(@Param("branchId") Long branchId, @Param("terminal") Collection<TransferStatus> terminal);

    @EntityGraph(attributePaths = {"sourceBranch", "destinationBranch", "createdBy", "items", "items.product"})
    @Query("select t from Transfer t where t.id = :id")
    Optional<Transfer> findDetailedById(@Param("id") Long id);
    @EntityGraph(attributePaths = {"sourceBranch", "destinationBranch", "createdBy", "items", "items.product"})
    List<Transfer> findBySourceBranchIdOrDestinationBranchIdOrderByCreatedAtDesc(Long source, Long destination);

    @EntityGraph(attributePaths = {"sourceBranch", "destinationBranch", "createdBy", "items", "items.product"})
    List<Transfer> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transfer t where t.id = :id")
    Optional<Transfer> lockById(@Param("id") Long id);
}
