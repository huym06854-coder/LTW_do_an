package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import vn.iotstar.domain.entity.Branch;
import vn.iotstar.domain.entity.Role;
import vn.iotstar.domain.entity.Transfer;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.TransferStatus;
import vn.iotstar.domain.enums.UserRole;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.TransferItemRepository;
import vn.iotstar.repository.TransferRepository;

class TransferServiceTest {

    @Test
    void managerCannotApproveTransferOutsideTheirBranch() {
        TransferRepository transfers = mock(TransferRepository.class);
        TransferItemRepository items = mock(TransferItemRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        BranchService branches = mock(BranchService.class);
        InventoryService inventory = mock(InventoryService.class);

        Branch source = new Branch();
        source.setId(10L);
        Branch managerBranch = new Branch();
        managerBranch.setId(20L);
        Transfer transfer = new Transfer();
        transfer.setSourceBranch(source);
        transfer.setStatus(TransferStatus.PENDING);
        when(transfers.lockById(5L)).thenReturn(Optional.of(transfer));

        Role managerRole = new Role();
        managerRole.setName(UserRole.MANAGER);
        User manager = new User();
        manager.setRole(managerRole);
        manager.setBranch(managerBranch);

        TransferService service = new TransferService(transfers, items, products, branches, inventory);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.approve(5L, manager));
        verifyNoInteractions(inventory);
    }
}
