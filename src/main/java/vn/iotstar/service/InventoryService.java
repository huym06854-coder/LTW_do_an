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

    public InventoryService(BranchStockRepository stocks, StockLogRepository logs, BranchRepository branches,
            ProductRepository products) {
        this.stocks = stocks;
        this.logs = logs;
        this.branches = branches;
        this.products = products;
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
        if (reason == null || reason.isBlank())
            throw new BusinessException("Phải ghi lý do điều chỉnh kho");
        move(branchId, productId, delta, StockMovementType.ADJUSTMENT, actor, null, reason);
    }
}
