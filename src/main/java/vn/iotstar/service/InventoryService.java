package vn.iotstar.service;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.stock.StockDto;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;

@Service
public class InventoryService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final BranchStockRepository stocks;
    private final StockLogRepository logs;
    private final BranchRepository branches;
    private final ProductRepository products;
    private final UserService users;

    public InventoryService(BranchStockRepository stocks, StockLogRepository logs, BranchRepository branches,
            ProductRepository products, UserService users) {
        this.stocks = stocks;
        this.logs = logs;
        this.branches = branches;
        this.products = products;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<StockDto> atBranch(Long branchId) {
        return stocks.findByBranchId(branchId).stream()
                .map(s -> new StockDto(s.getBranch().getId(), s.getProduct().getId(), s.getQuantity())).toList();
    }

    @Transactional(readOnly = true)
    public int available(Long branchId, Long productId) {
        return stocks.findByBranchIdAndProductId(branchId, productId).map(BranchStock::getQuantity).orElse(0);
    }

    @Transactional(readOnly = true)
    public List<vn.iotstar.dto.stock.StockRowDto> rowsAtBranch(Long branchId) {
        return stocks.findRowsByBranchId(branchId);
    }

    // All mutations go through this method and require a transaction.
    @Transactional
    public void move(Long branchId, Long productId, int delta, StockMovementType type, User actor, String reference,
            String reason) {
        if (delta == 0)
            throw new BusinessException("Số lượng điều chỉnh không được bằng 0");
        BranchStock stock = stocks.findByBranchIdAndProductId(branchId, productId).orElseGet(() -> {
            if (delta < 0)
                throw new BusinessException("Kho không có sản phẩm này");
            BranchStock created = new BranchStock();
            created.setBranch(branches.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại")));
            created.setProduct(products.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại")));
            created.setQuantity(0);
            return stocks.saveAndFlush(created);
        });
        // Acquire the write lock while refreshing, not with a version-checking query:
        // an earlier availability read may hold a stale version after another sale.
        // All quantity checks and changes below happen under this database lock.
        entityManager.refresh(stock, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        long next = (long) stock.getQuantity() + delta;
        if (next < 0)
            throw new BusinessException(
                    stock.getProduct().getName() + " còn " + stock.getQuantity() + ", không đủ số lượng yêu cầu");
        if (next > Integer.MAX_VALUE)
            throw new BusinessException("Số lượng tồn kho vượt giới hạn");
        stock.setQuantity((int) next);
        StockLog log = new StockLog();
        log.setBranch(stock.getBranch());
        log.setProduct(stock.getProduct());
        log.setQuantityChange(delta);
        log.setType(type);
        log.setPerformedBy(actor);
        log.setReferenceCode(reference);
        log.setNote(reason);
        logs.save(log);
    }

    @Transactional
    public void adminAdjust(Long branchId, Long productId, int delta, String reason, User actor) {
        if (actor == null) throw new org.springframework.security.access.AccessDeniedException("Tài khoản không còn hiệu lực");
        actor = users.profileById(actor.getId());
        if (actor == null || actor.getStatus() != UserStatus.ACTIVE
                || (actor.getRole().getName() != UserRole.ADMIN && actor.getRole().getName() != UserRole.MANAGER
                    && actor.getRole().getName() != UserRole.STAFF))
            throw new org.springframework.security.access.AccessDeniedException("Không được điều chỉnh kho");
        if (actor.getRole().getName() != UserRole.ADMIN
                && (actor.getBranch() == null || !actor.getBranch().getId().equals(branchId)))
            throw new org.springframework.security.access.AccessDeniedException("Không được điều chỉnh kho chi nhánh khác");
        var branch = branches.lockById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại"));
        entityManager.refresh(branch, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (branch.getStatus() != BranchStatus.ACTIVE)
            throw new BusinessException("Chi nhánh đã ngừng hoạt động");
        if (reason == null || reason.isBlank())
            throw new BusinessException("Phải ghi lý do điều chỉnh kho");
        if (reason.trim().length() > 500)
            throw new BusinessException("Lý do điều chỉnh không được quá 500 ký tự");
        move(branchId, productId, delta, StockMovementType.ADJUSTMENT, actor, null, reason.trim());
    }
}
