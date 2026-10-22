package vn.iotstar.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.domain.enums.*;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;

@Service
public class TransferService {
    private final TransferRepository transfers;
    private final TransferItemRepository items;
    private final ProductRepository products;
    private final BranchService branches;
    private final InventoryService inventory;

    public TransferService(TransferRepository transfers, TransferItemRepository items, ProductRepository products,
            BranchService branches, InventoryService inventory) {
        this.transfers = transfers;
        this.items = items;
        this.products = products;
        this.branches = branches;
        this.inventory = inventory;
    }

    @Transactional
    public Transfer create(Long sourceId, Long destinationId, Map<Long, Integer> productsAndQuantities, User actor) {
        if (sourceId.equals(destinationId))
            throw new BusinessException("Hai chi nhánh phải khác nhau");
        if (productsAndQuantities == null || productsAndQuantities.isEmpty())
            throw new BusinessException("Phiếu chuyển kho cần có sản phẩm");
        ensureBranchActor(actor, sourceId);
        Transfer t = new Transfer();
        t.setCode("TR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase());
        t.setSourceBranch(branches.getActive(sourceId));
        t.setDestinationBranch(branches.getActive(destinationId));
        t.setCreatedBy(actor);
        t.setStatus(TransferStatus.PENDING);
        for (var entry : productsAndQuantities.entrySet()) {
            if (entry.getValue() == null || entry.getValue() < 1)
                throw new BusinessException("Số lượng phải lớn hơn 0");
            TransferItem item = new TransferItem();
            item.setTransfer(t);
            item.setProduct(products.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại")));
            item.setQuantity(entry.getValue());
            t.getItems().add(item);
        }
        return transfers.save(t);
    }

    @Transactional(readOnly = true)
    public List<Transfer> inBranch(Long branchId) {
        return transfers.findBySourceBranchIdOrDestinationBranchIdOrderByCreatedAtDesc(branchId, branchId);
    }

    @Transactional
    public void approve(Long transferId, User actor) {
        Transfer t = lock(transferId);
        if (actor.getRole().getName() != UserRole.ADMIN && actor.getRole().getName() != UserRole.MANAGER)
            throw new BusinessException("Chỉ quản lý được duyệt chuyển kho");
        ensureBranchActor(actor, t.getSourceBranch().getId());
        if (t.getStatus() != TransferStatus.PENDING)
            throw new BusinessException("Phiếu không ở trạng thái chờ duyệt");
        // Always lock stock in product-id order to reduce deadlock probability.
        for (var i : sorted(t))
            inventory.move(t.getSourceBranch().getId(), i.getProduct().getId(), -i.getQuantity(),
                    StockMovementType.TRANSFER_OUT, actor, t.getCode(), "Duyệt chuyển kho");
        t.setApprovedBy(actor);
        t.setStatus(TransferStatus.APPROVED);
    }

    @Transactional
    public void ship(Long transferId, User actor) {
        Transfer t = lock(transferId);
        ensureBranchActor(actor, t.getSourceBranch().getId());
        if (t.getStatus() != TransferStatus.APPROVED)
            throw new BusinessException("Phiếu chưa được duyệt");
        t.setStatus(TransferStatus.IN_TRANSIT);
    }

    @Transactional
    public void complete(Long transferId, User actor) {
        Transfer t = lock(transferId);
        ensureBranchActor(actor, t.getDestinationBranch().getId());
        if (t.getStatus() != TransferStatus.IN_TRANSIT)
            throw new BusinessException("Hàng chưa ở trạng thái đang chuyển");
        for (var i : sorted(t))
            inventory.move(t.getDestinationBranch().getId(), i.getProduct().getId(), i.getQuantity(),
                    StockMovementType.TRANSFER_IN, actor, t.getCode(), "Nhập chuyển kho");
        t.setStatus(TransferStatus.COMPLETED);
    }

    @Transactional
    public void cancel(Long transferId, User actor) {
        Transfer t = lock(transferId);
        if (t.getStatus() == TransferStatus.COMPLETED || t.getStatus() == TransferStatus.CANCELLED
                || t.getStatus() == TransferStatus.IN_TRANSIT)
            throw new BusinessException("Không thể hủy phiếu đang vận chuyển, hoàn tất hoặc đã hủy");
        ensureBranchActor(actor, t.getSourceBranch().getId());
        if (t.getStatus() == TransferStatus.APPROVED) {
            for (var i : sorted(t))
                inventory.move(t.getSourceBranch().getId(), i.getProduct().getId(), i.getQuantity(),
                        StockMovementType.RETURN, actor, t.getCode(), "Hoàn kho do hủy chuyển kho");
        }
        t.setStatus(TransferStatus.CANCELLED);
    }

    private Transfer lock(Long id) {
        return transfers.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu chuyển kho"));
    }

    private List<TransferItem> sorted(Transfer t) {
        return t.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).toList();
    }

    private void ensureBranchActor(User actor, Long branchId) {
        if (actor.getRole().getName() != UserRole.ADMIN
                && (actor.getBranch() == null || !actor.getBranch().getId().equals(branchId)))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không thuộc chi nhánh được phép xử lý phiếu");
    }
}
