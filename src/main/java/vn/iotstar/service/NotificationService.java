package vn.iotstar.service;

import java.time.LocalDateTime;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.NotificationRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.domain.enums.*;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final SimpMessagingTemplate socket;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, SimpMessagingTemplate socket,
            UserRepository users) {
        this.notifications = notifications;
        this.socket = socket;
        this.users = users;
    }

    @Transactional
    public void newBranchOrder(Long branchId, String orderNumber) {
        for (User operator : users.findOrderOperators(branchId, UserStatus.ACTIVE, UserRole.ADMIN,
                java.util.List.of(UserRole.STAFF, UserRole.MANAGER)))
            create(operator, "Đơn hàng mới", orderNumber + " vừa được đặt tại chi nhánh", "NEW_ORDER");
    }

    @Transactional(readOnly = true)
    public Page<Notification> forUser(Long id, Pageable page) {
        return notifications.findByUserIdOrderByCreatedAtDesc(id, page);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notifications.findById(notificationId).filter(x -> x.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại"));
        n.setReadAt(LocalDateTime.now());
    }

    @Transactional
    public Notification create(User to, String title, String message, String type) {
        Notification n = new Notification();
        n.setUser(to);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        Notification saved = notifications.save(n);
        // The socket event must not be sent if this DB transaction rolls back.
        Runnable publish = () -> {
            try {
                socket.convertAndSendToUser(to.getUsername(), "/queue/notifications",
                        java.util.Map.of("id", saved.getId(), "title", title, "message", message, "type", type));
            } catch (RuntimeException error) {
                System.err.println("WebSocket notification dispatch failed; notification remains in DB");
            }
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publish.run();
                        }
                    });
        } else
            publish.run();
        return saved;
    }
}
