package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.Order;
import vn.iotstar.domain.enums.*;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
        Optional<Order> findByUserIdAndCheckoutToken(Long userId, String checkoutToken);

        Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        Page<Order> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        Long userId, java.time.LocalDateTime from, java.time.LocalDateTime to, Pageable pageable);

        Page<Order> findByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        Long userId, OrderStatus status, java.time.LocalDateTime from, java.time.LocalDateTime to,
                        Pageable pageable);

        Page<Order> findByUserIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        Long userId, Collection<OrderStatus> statuses, java.time.LocalDateTime from,
                        java.time.LocalDateTime to, Pageable pageable);

        Optional<Order> findByIdAndUserId(Long id, Long userId);

        Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

        List<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId);

        @Query("select count(i) from OrderItem i where i.order.user.id = :userId and i.product.id = :productId and i.order.status = :status")
        long countCompletedPurchases(@Param("userId") Long userId, @Param("productId") Long productId,
                        @Param("status") OrderStatus status);

        @Query("select coalesce(sum(o.totalAmount),0) from Order o where (:branchId is null or o.branch.id = :branchId) and o.status = :status and o.completedAt >= :start and o.completedAt < :end")
        java.math.BigDecimal revenueCompleted(@Param("branchId") Long branchId,
                        @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end,
                        @Param("status") OrderStatus status);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select o from Order o where o.id = :id")
        Optional<Order> lockById(@Param("id") Long id);
}
