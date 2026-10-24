package vn.iotstar.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.dto.cart.*;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;

@Service
public class CartService {
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductService products;
    private final UserRepository users;
    private final BranchService branches;
    private final InventoryService inventory;

    public CartService(CartRepository carts, CartItemRepository items, ProductService products, UserRepository users,
            BranchService branches, InventoryService inventory) {
        this.carts = carts;
        this.items = items;
        this.products = products;
        this.users = users;
        this.branches = branches;
        this.inventory = inventory;
    }

    @Transactional
    public Cart getOrCreate(Long userId) {
        User user = users.lockForAddressBook(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        return carts.lockByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(user);
            return carts.saveAndFlush(c);
        });
    }

    @Transactional(readOnly = true)
    public CartDto view(Long userId) {
        var optional = carts.findByUserId(userId);
        if (optional.isEmpty())
            return new CartDto(null, List.of(), BigDecimal.ZERO);
        var cart = optional.get();
        var lines = items.findByCartIdOrderByIdAsc(cart.getId());
        List<CartItemDto> dto = lines.stream()
                .map(i -> new CartItemDto(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getProduct().getPrice(),
                        i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())),
                        i.getProduct().getImages().stream().filter(ProductImage::isPrimary).findFirst()
                                .map(ProductImage::getImageUrl).orElse(null),
                        cart.getBranch() == null ? null
                                : inventory.available(cart.getBranch().getId(), i.getProduct().getId())))
                .toList();
        var total = dto.stream().map(CartItemDto::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto(cart.getBranch() == null ? null : cart.getBranch().getId(), dto, total,
                cart.getBranch() == null ? null : cart.getBranch().getName(),
                cart.getBranch() == null ? null : cart.getBranch().getAddress());
    }

    @Transactional(readOnly = true)
    public CartDto previewPurchase(Long productId, int quantity, Long branchId) {
        if (quantity < 1 || quantity > 99)
            throw new BusinessException("Số lượng phải từ 1 đến 99");
        var product = products.detail(productId);
        var subtotal = product.price().multiply(BigDecimal.valueOf(quantity));
        var branch = branchId == null ? null : branches.getActive(branchId);
        Integer available = branch == null ? null : inventory.available(branchId, productId);
        var line = new CartItemDto(productId, product.name(), quantity, product.price(), subtotal,
                product.imageUrl(), available);
        return new CartDto(branchId, List.of(line), subtotal,
                branch == null ? null : branch.getName(), branch == null ? null : branch.getAddress());
    }

    @Transactional
    public void chooseBranch(Long userId, Long branchId) {
        Cart cart = getOrCreate(userId);
        Branch branch = branches.getActive(branchId);
        List<String> shortages = new ArrayList<>();
        for (CartItem line : items.findByCartIdOrderByIdAsc(cart.getId())) {
            int available = inventory.available(branchId, line.getProduct().getId());
            if (available < line.getQuantity())
                shortages.add(stockMessage(line.getProduct(), available, line.getQuantity()));
        }
        if (!shortages.isEmpty())
            throw new BusinessException(branch.getName() + " chưa đủ hàng: " + String.join("; ", shortages));
        cart.setBranch(branch);
    }

    @Transactional
    public void add(Long userId, Long productId, int quantity) {
        if (quantity < 1 || quantity > 99)
            throw new BusinessException("Số lượng phải từ 1 đến 99");
        Cart cart = getOrCreate(userId);
        Product product = products.findActive(productId);
        CartItem line = items.findByCartIdAndProductId(cart.getId(), productId).orElseGet(() -> {
            CartItem i = new CartItem();
            i.setCart(cart);
            i.setProduct(product);
            i.setQuantity(0);
            return i;
        });
        if ((long) line.getQuantity() + quantity > 99)
            throw new BusinessException("Tối đa 99 sản phẩm mỗi loại");
        int requested = line.getQuantity() + quantity;
        if (cart.getBranch() != null) {
            int available = inventory.available(cart.getBranch().getId(), productId);
            if (available < requested)
                throw new BusinessException(stockMessage(product, available, requested));
        }
        line.setQuantity(requested);
        items.save(line);
    }

    @Transactional
    public void update(Long userId, Long productId, int quantity) {
        if (quantity < 0 || quantity > 99)
            throw new BusinessException("Số lượng không hợp lệ");
        Cart cart = carts.lockByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Không có giỏ hàng"));
        CartItem line = items.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ"));
        if (quantity == 0) {
            items.delete(line);
            return;
        }
        products.findActive(productId);
        if (cart.getBranch() != null) {
            int available = inventory.available(cart.getBranch().getId(), productId);
            if (available < quantity)
                throw new BusinessException(stockMessage(line.getProduct(), available, quantity));
        }
        line.setQuantity(quantity);
    }

    private static String stockMessage(Product product, int available, int quantity) {
        return product.getName() + " còn " + available + ", bạn đang chọn " + quantity;
    }
}
