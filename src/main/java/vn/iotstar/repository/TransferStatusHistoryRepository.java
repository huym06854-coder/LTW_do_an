package vn.iotstar.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import vn.iotstar.domain.entity.*;

public interface TransferStatusHistoryRepository extends JpaRepository<TransferStatusHistory, Long> {
    @EntityGraph(attributePaths = "changedBy")
    List<TransferStatusHistory> findByTransferIdOrderByCreatedAtAscIdAsc(Long transferId);
}

