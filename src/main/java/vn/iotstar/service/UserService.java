package vn.iotstar.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.auth.RegisterDto;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;

@Service
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public User byLogin(String login) {
        return users.findByLogin(login).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    @Transactional(readOnly = true)
    public User byId(Long id) {
        return users.findWithRoleById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    @Transactional(readOnly = true)
    public User profileById(Long id) {
        return users.findWithRoleById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    @Transactional(readOnly = true)
    public User branchOperator(Long id) {
        var user = byId(id);
        var role = user.getRole().getName();
        if (user.getStatus() != UserStatus.ACTIVE
                || (role != UserRole.ADMIN && role != UserRole.MANAGER && role != UserRole.STAFF))
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản không được vận hành chi nhánh");
        if (user.getBranch() == null)
            throw new BusinessException("Chưa được phân công chi nhánh. Vui lòng liên hệ quản trị viên.");
        if (user.getBranch().getStatus() != BranchStatus.ACTIVE)
            throw new BusinessException("Chi nhánh được phân công đã ngừng hoạt động");
        return user;
    }

    @Transactional
    public User lockForCheckout(Long id) {
        return users.lockForAddressBook(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    @Transactional
    public User register(RegisterDto dto) {
        if (dto.username() == null || dto.username().isBlank() || dto.fullName() == null || dto.fullName().isBlank()
                || dto.email() == null || dto.email().isBlank())
            throw new BusinessException("Vui lòng nhập đầy đủ thông tin đăng ký");
        String username = dto.username().trim();
        String email = dto.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (username.contains("@"))
            throw new BusinessException("Tên đăng nhập không được chứa @");
        if (users.existsByUsernameIgnoreCase(username) || users.existsByEmailIgnoreCase(email))
            throw new BusinessException(
                    "Email hoặc tên đăng nhập đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng thông tin khác.");
        if (dto.password() == null || dto.password().length() < 8)
            throw new BusinessException("Mật khẩu phải có ít nhất 8 ký tự");
        Role customer = roles.findByName(UserRole.CUSTOMER)
                .orElseThrow(() -> new BusinessException("Chưa cấu hình vai trò CUSTOMER"));
        User u = new User();
        u.setFullName(dto.fullName().trim());
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(dto.phone());
        u.setPassword(encoder.encode(dto.password()));
        u.setRole(customer);
        return users.save(u);
    }
}
