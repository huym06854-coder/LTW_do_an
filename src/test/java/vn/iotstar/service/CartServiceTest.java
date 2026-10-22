package vn.iotstar.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.iotstar.domain.entity.*;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.repository.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CartServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final ProductService products = mock(ProductService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BranchService branches = mock(BranchService.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final CartService service = new CartService(carts, items, products, users, branches, inventory);
    private Cart cart;

    @BeforeEach
    void setUp() {
        User user = new User(); user.setId(1L);
        cart = new Cart(); cart.setId(10L); cart.setUser(user);
        when(users.lockForAddressBook(1L)).thenReturn(Optional.of(user));
        when(carts.lockByUserId(1L)).thenReturn(Optional.of(cart));
    }

    @Test
    void choosingBranchReportsEveryShortageAndKeepsPreviousSelection() {
        Branch previous = new Branch(); previous.setId(2L); cart.setBranch(previous);
        Branch next = new Branch(); next.setId(3L); next.setName("Cửa hàng Trung tâm");
        when(branches.getActive(3L)).thenReturn(next);
        CartItem laptop = line(101L, "Laptop ASUS", 3);
        CartItem monitor = line(102L, "Màn hình Dell", 2);
        when(items.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(laptop, monitor));
        when(inventory.available(3L, 101L)).thenReturn(1);
        when(inventory.available(3L, 102L)).thenReturn(0);

        assertThatThrownBy(() -> service.chooseBranch(1L, 3L)).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Laptop ASUS còn 1, bạn đang chọn 3")
            .hasMessageContaining("Màn hình Dell còn 0, bạn đang chọn 2");
        assertThat(cart.getBranch()).isSameAs(previous);
    }

    @Test
    void failedQuantityChangePreservesCartAndExplicitRemovalDoesNotRequireStock() {
        Branch branch = new Branch(); branch.setId(2L); cart.setBranch(branch);
        CartItem laptop = line(101L, "Laptop ASUS", 1);
        when(items.findByCartIdAndProductId(10L, 101L)).thenReturn(Optional.of(laptop));
        when(products.findActive(101L)).thenReturn(laptop.getProduct());
        when(inventory.available(2L, 101L)).thenReturn(2);

        assertThatThrownBy(() -> service.update(1L, 101L, 3)).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Laptop ASUS còn 2, bạn đang chọn 3");
        assertThat(laptop.getQuantity()).isEqualTo(1);

        clearInvocations(inventory);
        service.update(1L, 101L, 0);
        verify(items).delete(laptop);
        verifyNoInteractions(inventory);
    }

    private CartItem line(long productId, String name, int quantity) {
        Product product = new Product(); product.setId(productId); product.setName(name);
        CartItem line = new CartItem(); line.setCart(cart); line.setProduct(product); line.setQuantity(quantity);
        return line;
    }
}
