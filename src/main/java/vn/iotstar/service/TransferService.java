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
    private final ProductRepository products;
    private final BranchService branches;
    private final InventoryService inventory;
    private final TransferStatusHistoryRepository histories;
    private final UserService users;

    public TransferService(TransferRepository transfers, ProductRepository products,
            BranchService branches, InventoryService inventory, TransferStatusHistoryRepository histories, UserService users) {
        this.transfers = transfers;
        this.products = products;
        this.branches = branches;
        this.inventory = inventory;
        this.histories = histories;
        this.users = users;
    }

    @Transactional
    public Transfer create(Long sourceId, Long destinationId, Map<Long, Integer> productsAndQuantities, User actor) {
        actor = current(actor);
        if (sourceId.equals(destinationId))
            throw new BusinessException("Hai chi nhánh phải khác nhau");
        if (productsAndQuantities == null || productsAndQuantities.isEmpty())
            throw new BusinessException("Phiếu chuyển kho cần có sản phẩm");
        ensureBranchActor(actor, sourceId);
        Transfer t = new Transfer();
        t.setCode("TR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase());
        Map<Long, Branch> locked = new HashMap<>();
        for (Long id : java.util.stream.Stream.of(sourceId, destinationId).sorted().toList())
            locked.put(id, branches.lockActive(id));
        t.setSourceBranch(locked.get(sourceId));
        t.setDestinationBranch(locked.get(destinationId));
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
        transfers.save(t);
        history(t, null, TransferStatus.PENDING, actor, "Tạo yêu cầu chuyển kho");
        return t;
    }

    @Transactional(readOnly = true)
    public List<Transfer> inBranch(Long branchId) {
        return transfers.findBySourceBranchIdOrDestinationBranchIdOrderByCreatedAtDesc(branchId, branchId);
    }

    @Transactional
    public void approve(Long transferId, User actor) {
        actor = current(actor);
        Transfer t = lock(transferId);
        if (actor.getRole().getName() != UserRole.ADMIN && actor.getRole().getName() != UserRole.MANAGER)
            throw new BusinessException("Chỉ quản lý được duyệt chuyển kho");
        ensureBranchActor(actor, t.getSourceBranch().getId());
        if (t.getStatus() != TransferStatus.PENDING)
            throw new BusinessException("Phiếu không ở trạng thái chờ duyệt");
        branches.getActive(t.getSourceBranch().getId());
        branches.getActive(t.getDestinationBranch().getId());
        // Always lock stock in product-id order to reduce deadlock probability.
        for (var i : sorted(t))
            inventory.move(t.getSourceBranch().getId(), i.getProduct().getId(), -i.getQuantity(),
                    StockMovementType.TRANSFER_OUT, actor, t.getCode(), "Duyệt chuyển kho");
        t.setApprovedBy(actor);
        t.setStatus(TransferStatus.APPROVED);
        history(t, TransferStatus.PENDING, TransferStatus.APPROVED, actor, "Duyệt phiếu và trừ kho nguồn");
    }

    @Transactional
    public void ship(Long transferId, User actor) {
        actor = current(actor);
        Transfer t = lock(transferId);
        ensureBranchActor(actor, t.getSourceBranch().getId());
        if (t.getStatus() != TransferStatus.APPROVED)
            throw new BusinessException("Phiếu chưa được duyệt");
        t.setStatus(TransferStatus.IN_TRANSIT);
        history(t, TransferStatus.APPROVED, TransferStatus.IN_TRANSIT, actor, "Xác nhận bàn giao hàng vận chuyển");
    }

    @Transactional
    public void complete(Long transferId, User actor) {
        actor = current(actor);
        Transfer t = lock(transferId);
        ensureBranchActor(actor, t.getDestinationBranch().getId());
        if (t.getStatus() != TransferStatus.IN_TRANSIT)
            throw new BusinessException("Hàng chưa ở trạng thái đang chuyển");
        for (var i : sorted(t))
            inventory.move(t.getDestinationBranch().getId(), i.getProduct().getId(), i.getQuantity(),
                    StockMovementType.TRANSFER_IN, actor, t.getCode(), "Nhập chuyển kho");
        t.setStatus(TransferStatus.COMPLETED);
        history(t, TransferStatus.IN_TRANSIT, TransferStatus.COMPLETED, actor, "Xác nhận nhận đủ hàng và nhập kho đích");
    }

    @Transactional
    public void cancel(Long transferId, User actor) {
        actor = current(actor);
        Transfer t = lock(transferId);
        TransferStatus previous = t.getStatus();
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
        history(t, previous, TransferStatus.CANCELLED, actor, "Hủy phiếu chuyển kho");
    }

    @Transactional(readOnly = true)
    public vn.iotstar.dto.stock.TransferDetailDto detail(Long id, User actor) {
        actor = current(actor);
        Transfer transfer = transfers.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu chuyển kho"));
        if (actor.getRole().getName() != UserRole.ADMIN && (actor.getBranch() == null
                || (!actor.getBranch().getId().equals(transfer.getSourceBranch().getId())
                    && !actor.getBranch().getId().equals(transfer.getDestinationBranch().getId()))))
            throw new org.springframework.security.access.AccessDeniedException("Phiếu không thuộc chi nhánh của bạn");
        return new vn.iotstar.dto.stock.TransferDetailDto(transfer, histories.findByTransferIdOrderByCreatedAtAscIdAsc(id));
    }

    private User current(User actor) {
        if (actor == null) throw new org.springframework.security.access.AccessDeniedException("Tài khoản không còn hiệu lực");
        actor = users.profileById(actor.getId());
        if (actor.getStatus() != UserStatus.ACTIVE || actor.getRole().getName() == UserRole.CUSTOMER)
            throw new org.springframework.security.access.AccessDeniedException("Không được xử lý chuyển kho");
        if (actor.getRole().getName() != UserRole.ADMIN && (actor.getBranch() == null
                || actor.getBranch().getStatus() != BranchStatus.ACTIVE))
            throw new org.springframework.security.access.AccessDeniedException("Chi nhánh chưa được phân công hoặc đã ngừng hoạt động");
        return actor;
    }

    private void history(Transfer transfer, TransferStatus previous, TransferStatus next, User actor, String note) {
        var event = new TransferStatusHistory();
        event.setTransfer(transfer); event.setPreviousStatus(previous); event.setNewStatus(next);
        event.setChangedBy(actor); event.setNote(note); histories.save(event);
    }

    private Transfer lock(Long id) {
        return transfers.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu chuyển kho"));
    }

    private List<TransferItem> sorted(Transfer t) {
        return t.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).toList();
    }

    private void ensureBranchActor(User actor, Long branchId) {
        if (actor == null || actor.getStatus() != UserStatus.ACTIVE
                || (actor.getRole().getName() != UserRole.ADMIN && actor.getRole().getName() != UserRole.MANAGER
                    && actor.getRole().getName() != UserRole.STAFF))
            throw new org.springframework.security.access.AccessDeniedException("Không được xử lý chuyển kho");
        if (actor.getRole().getName() != UserRole.ADMIN
                && (actor.getBranch() == null || !actor.getBranch().getId().equals(branchId)))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không thuộc chi nhánh được phép xử lý phiếu");
    }
}
