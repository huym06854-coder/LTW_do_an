package vn.iotstar.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.repository.OrderRepository;

@Service
public class ReportService {
    private final OrderRepository orders;

    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public BigDecimal revenueByCompletedDate(Long branchId, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start))
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
        return orders.revenueCompleted(branchId, start, end, vn.iotstar.domain.enums.OrderStatus.COMPLETED);
    }
}
