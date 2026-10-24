package vn.iotstar.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.Branch;
import vn.iotstar.domain.enums.BranchStatus;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.stock.BranchSummaryDto;

@Service
public class BranchService {
    private final BranchRepository repository;
    private final UserRepository users;
    private final OrderRepository orders;
    private final TransferRepository transfers;
    private final BranchStockRepository stocks;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public BranchService(BranchRepository repository, UserRepository users, OrderRepository orders,
            TransferRepository transfers, BranchStockRepository stocks) {
        this.repository = repository;
        this.users = users; this.orders = orders; this.transfers = transfers; this.stocks = stocks;
    }

    @Transactional(readOnly = true)
    public Branch get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
    }

    @Transactional(readOnly = true)
    public long pendingTransferCount() {
        return transfers.countByStatus(TransferStatus.PENDING);
    }

    @Transactional
    public Branch lockActive(Long id) {
        var branch = lock(id);
        if (branch.getStatus() != BranchStatus.ACTIVE) throw new BusinessException("Chi nhánh đã ngừng hoạt động");
        return branch;
    }

    private Branch lock(Long id) {
        var branch = repository.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        entityManager.refresh(branch, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return branch;
    }

    @Transactional
    public void changeStatus(Long id, BranchStatus next) {
        if (next == null) throw new BusinessException("Vui lòng chọn trạng thái chi nhánh");
        var branch = lock(id);
        if (branch.getStatus() == next) return;
        if (next == BranchStatus.INACTIVE && (orders.existsByBranchIdAndStatusNotIn(id,
                List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.RETURNED))
                || transfers.countOpenAtBranch(id, List.of(TransferStatus.COMPLETED, TransferStatus.CANCELLED)) > 0))
            throw new BusinessException("Cần xử lý xong đơn hàng và phiếu chuyển kho đang mở trước khi ngừng hoạt động chi nhánh");
        branch.setStatus(next);
    }

    @Transactional(readOnly = true)
    public List<BranchSummaryDto> summaries() {
        Map<Long, long[]> counts = new HashMap<>();
        for (var row : users.countEmployeesByBranch(List.of(UserRole.MANAGER, UserRole.STAFF))) {
            var total = counts.computeIfAbsent((Long) row[0], k -> new long[5]);
            total[row[1] == UserRole.MANAGER ? 0 : 1] = ((Number) row[2]).longValue();
        }
        for (var row : orders.countByBranchForStatus(OrderStatus.PENDING))
            counts.computeIfAbsent((Long) row[0], k -> new long[5])[2] = ((Number) row[1]).longValue();
        for (var row : transfers.countRoutesByStatus(TransferStatus.PENDING)) {
            counts.computeIfAbsent((Long) row[0], k -> new long[5])[3] += ((Number) row[2]).longValue();
            counts.computeIfAbsent((Long) row[1], k -> new long[5])[3] += ((Number) row[2]).longValue();
        }
        for (var row : stocks.countLowStockByBranch())
            counts.computeIfAbsent((Long) row[0], k -> new long[5])[4] = ((Number) row[1]).longValue();
        return repository.findAllByOrderByNameAsc().stream().map(b -> {
            var c = counts.getOrDefault(b.getId(), new long[5]);
            return new BranchSummaryDto(b.getId(), b.getCode(), b.getName(), b.getAddress(), b.getStatus(),
                    c[0], c[1], c[2], c[3], c[4]);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Branch> active() {
        return repository.findByStatusOrderByNameAsc(BranchStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Branch> all() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Branch getActive(Long id) {
        Branch b = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        if (b.getStatus() != BranchStatus.ACTIVE)
            throw new BusinessException("Chi nhánh đã ngừng hoạt động");
        return b;
    }

    @Transactional
    public Branch create(String code, String name, String address) {
        if (code == null || code.isBlank() || code.trim().length() > 30)
            throw new BusinessException("Mã chi nhánh phải từ 1 đến 30 ký tự");
        validateDetails(name, address);
        if (repository.existsByCodeIgnoreCase(code.trim()))
            throw new BusinessException("Mã chi nhánh đã được sử dụng");
        Branch b = new Branch();
        b.setCode(code.trim());
        b.setName(name.trim());
        b.setAddress(address.trim());
        return repository.save(b);
    }

    @Transactional
    public Branch updateDetails(Long id, String name, String address) {
        Branch branch = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        validateDetails(name, address);
        branch.setName(name.trim());
        branch.setAddress(address.trim());
        return branch;
    }

    private void validateDetails(String name, String address) {
        if (name == null || name.isBlank() || address == null || address.isBlank())
            throw new BusinessException("Tên và địa chỉ chi nhánh không được để trống");
        if (name.trim().length() > 160 || address.trim().length() > 500)
            throw new BusinessException("Tên tối đa 160 ký tự, địa chỉ tối đa 500 ký tự");
    }
}
