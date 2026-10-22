package vn.iotstar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.enums.UserStatus;
import vn.iotstar.exception.*;
import vn.iotstar.repository.UserRepository;

@Service
public class AdminUserService {
    private final UserRepository users;

    public AdminUserService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public void changeStatus(Long targetId, Long actorId, UserStatus next) {
        if (targetId.equals(actorId))
            throw new BusinessException("Không được tự khóa tài khoản đang sử dụng");
        var target = users.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
        target.setStatus(next);
    }
}
