package vn.iotstar.service;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.auth.EmployeeCreateDto;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final BranchService branches;
    private final PasswordEncoder encoder;
    private final EmployeeAssignmentHistoryRepository histories;

    public AdminUserService(UserRepository users, RoleRepository roles, BranchService branches,
            PasswordEncoder encoder, EmployeeAssignmentHistoryRepository histories) {
        this.users = users; this.roles = roles; this.branches = branches;
        this.encoder = encoder; this.histories = histories;
    }

    @Transactional(readOnly = true)
    public Page<User> search(String group, String query, UserRole role, UserStatus status, Long branchId, int page) {
        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return users.findAll((root, criteria, cb) -> {
            var filters = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if ("employees".equals(group)) filters.add(root.get("role").get("name").in(UserRole.STAFF, UserRole.MANAGER));
            else if ("customers".equals(group)) filters.add(cb.equal(root.get("role").get("name"), UserRole.CUSTOMER));
            if (role != null) filters.add(cb.equal(root.get("role").get("name"), role));
            if (status != null) filters.add(cb.equal(root.get("status"), status));
            if (branchId != null) filters.add(cb.equal(root.get("branch").get("id"), branchId));
            if (!keyword.isBlank()) {
                String literal = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                filters.add(cb.or(cb.like(cb.lower(root.get("fullName")), literal, '\\'),
                        cb.like(cb.lower(root.get("username")), literal, '\\'),
                        cb.like(cb.lower(root.get("email")), literal, '\\')));
            }
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, PageRequest.of(Math.max(0, page), 15, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional
    public void createEmployee(EmployeeCreateDto dto, Long actorId) {
        User actor = requireAdmin(actorId);
        employeeRole(dto.getRole());
        var branch = branches.lockActive(dto.getBranchId());
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(username) || users.existsByEmailIgnoreCase(email))
            throw new BusinessException("Tên đăng nhập hoặc email đã được sử dụng");
        if (dto.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("Mật khẩu quá dài. Vui lòng sử dụng mật khẩu ngắn hơn.");
        var account = new User();
        account.setFullName(dto.getFullName().trim()); account.setUsername(username); account.setEmail(email);
        account.setPassword(encoder.encode(dto.getPassword()));
        account.setRole(roles.findByName(dto.getRole()).orElseThrow(() -> new BusinessException("Chưa cấu hình vai trò này")));
        account.setBranch(branch);
        users.save(account);
        record(account, null, null, null, actor, "Tạo tài khoản nhân sự");
    }

    @Transactional
    public void assignBranch(Long targetId, Long branchId, Long actorId) {
        var target = lock(targetId);
        changeAssignment(targetId, branchId, target.getRole().getName(), actorId);
    }

    @Transactional
    public void changeAssignment(Long targetId, Long branchId, UserRole role, Long actorId) {
        User actor = requireAdmin(actorId);
        employeeRole(role);
        var target = lock(targetId);
        employeeRole(target.getRole().getName());
        var branch = branches.lockActive(branchId);
        var previousBranch = target.getBranch();
        var previousRole = target.getRole().getName();
        if (previousRole == role && previousBranch != null && previousBranch.getId().equals(branchId)) return;
        target.setRole(roles.findByName(role).orElseThrow(() -> new BusinessException("Chưa cấu hình vai trò này")));
        target.setBranch(branch);
        record(target, previousRole, previousBranch, target.getStatus(), actor, "Cập nhật vai trò / chi nhánh");
    }

    @Transactional
    public void changeStatus(Long targetId, Long actorId, UserStatus next) {
        User actor = requireAdmin(actorId);
        if (targetId.equals(actorId)) throw new BusinessException("Không được tự khóa tài khoản đang sử dụng");
        if (next == null) throw new BusinessException("Vui lòng chọn trạng thái tài khoản");
        var target = lock(targetId);
        var previous = target.getStatus();
        if (previous == next) return;
        target.setStatus(next);
        record(target, target.getRole().getName(), target.getBranch(), previous, actor, "Cập nhật trạng thái tài khoản");
    }

    @Transactional(readOnly = true)
    public List<EmployeeAssignmentHistory> history(Long id) {
        return histories.findTop50ByUserIdOrderByCreatedAtDescIdDesc(id);
    }

    private User requireAdmin(Long actorId) {
        var actor = users.findWithRoleById(actorId).orElseThrow(() -> new AccessDeniedException("Tài khoản không còn hiệu lực"));
        if (actor.getStatus() != UserStatus.ACTIVE || actor.getRole().getName() != UserRole.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên đang hoạt động được quản lý nhân sự");
        return actor;
    }

    private User lock(Long id) {
        return users.lockForAddressBook(id).orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
    }

    private void employeeRole(UserRole role) {
        if (role != UserRole.STAFF && role != UserRole.MANAGER)
            throw new BusinessException("Chỉ phân công vai trò Nhân viên hoặc Quản lý chi nhánh tại đây");
    }

    private void record(User target, UserRole oldRole, Branch oldBranch, UserStatus oldStatus, User actor, String note) {
        var event = new EmployeeAssignmentHistory();
        event.setUser(target); event.setChangedBy(actor);
        event.setPreviousRole(oldRole); event.setNewRole(target.getRole().getName());
        event.setPreviousBranch(oldBranch); event.setNewBranch(target.getBranch());
        event.setPreviousStatus(oldStatus); event.setNewStatus(target.getStatus()); event.setNote(note);
        histories.save(event);
    }
}
