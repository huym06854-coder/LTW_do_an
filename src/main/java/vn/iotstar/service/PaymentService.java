package vn.iotstar.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.Payment;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.*;
import vn.iotstar.domain.enums.PaymentStatus;
import vn.iotstar.exception.*;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.OrderRepository;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final OrderRepository orders;
    private final UserService users;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public PaymentService(PaymentRepository repository,OrderRepository orders,UserService users) {
        this.repository = repository;this.orders=orders;this.users=users;
    }

    // Manual confirmation ONLY. External callbacks require signature verification
    // and a payment-provider integration.
    @Transactional
    public void confirmManualPayment(Long paymentId, String transactionId,User actor) {
        if (transactionId == null || transactionId.isBlank() || transactionId.length()>120)
            throw new BusinessException("Thiếu mã giao dịch");
        Payment reference = repository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán"));
        var order=orders.lockById(reference.getOrder().getId()).orElseThrow();
        actor=users.profileById(actor.getId());
        if(actor.getRole().getName()==UserRole.CUSTOMER||actor.getRole().getName()!=UserRole.ADMIN
                &&(actor.getBranch()==null||!actor.getBranch().getId().equals(order.getBranch().getId())))
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền xác nhận thanh toán cho đơn này");
        Payment payment=repository.lockById(paymentId).orElseThrow();
        entityManager.refresh(payment,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(order.getStatus()==OrderStatus.CANCELLED||order.getStatus()==OrderStatus.RETURNED||order.getStatus()==OrderStatus.RETURNING)
            throw new BusinessException("Không thể thu tiền cho đơn đã hủy hoặc đang hoàn hàng");
        if(payment.getMethod()==PaymentMethod.CASH_ON_DELIVERY)
            throw new BusinessException("Vui lòng xác nhận thu COD khi bàn giao và hoàn tất đơn hàng");
        if (payment.getStatus() != PaymentStatus.PENDING)
            throw new BusinessException("Chỉ xác nhận khoản thanh toán đang chờ; không thể thu lại khoản đã hoàn tiền");
        if (repository.findByTransactionId(transactionId.trim()).isPresent())
            throw new BusinessException("Giao dịch đã được sử dụng");
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(transactionId.trim());
        payment.setPaidAt(LocalDateTime.now());
        payment.setConfirmedBy(actor);
    }
}
