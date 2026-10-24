package vn.iotstar.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.*;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.order.*;
import vn.iotstar.exception.*;
import vn.iotstar.repository.*;

@Service
public class OrderService {
    private static final DateTimeFormatter PROFILE_ORDER_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final OrderRepository orders;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final InventoryService inventory;
    private final UserService users;
    private final BranchService branches;
    private final CouponService coupons;
    private final PaymentRepository payments;
    private final OrderStatusHistoryRepository histories;
    private final NotificationService notifications;
    private final AddressService addresses;
    private final ShippingFeeService shipping;
    private final ProductService products;

    public OrderService(OrderRepository orders, CartRepository carts, CartItemRepository cartItems,
            InventoryService inventory, UserService users, BranchService branches, CouponService coupons,
            PaymentRepository payments, OrderStatusHistoryRepository histories, NotificationService notifications,
            AddressService addresses, ShippingFeeService shipping, ProductService products) {
        this.orders = orders;
        this.carts = carts;
        this.cartItems = cartItems;
        this.inventory = inventory;
        this.users = users;
        this.branches = branches;
        this.coupons = coupons;
        this.payments = payments;
        this.histories = histories;
        this.notifications = notifications;
        this.addresses = addresses;
        this.shipping = shipping;
        this.products = products;
    }

    @Transactional
    public OrderDto checkout(Long userId, CheckoutDto dto) {
        users.lockForCheckout(userId);
        if (dto.checkoutToken() != null && !dto.checkoutToken().isBlank()) {
            if (!dto.checkoutToken().matches("[A-Za-z0-9-]{16,64}"))
                throw new BusinessException("Phiên đặt hàng không hợp lệ");
            var previous = orders.findByUserIdAndCheckoutToken(userId, dto.checkoutToken());
            if (previous.isPresent())
                return toDto(previous.get());
        }
        if (dto.deliveryMethod() == null)
            throw new CheckoutValidationException("deliveryMethod", "Vui lòng chọn hình thức nhận hàng");
        if (dto.paymentMethod() != PaymentMethod.CASH_ON_DELIVERY)
            throw new BusinessException("Hiện chỉ hỗ trợ thanh toán khi nhận hàng");
        Recipient recipient = recipient(userId, dto);
        Cart cart = dto.buyNowProductId() == null ? carts.lockByUserId(userId)
                .orElseThrow(() -> new BusinessException("Giỏ hàng trống")) : null;
        if (cart != null && (cart.getBranch() == null || !cart.getBranch().getId().equals(dto.branchId())))
            throw new BusinessException("Vui lòng chọn chi nhánh cho giỏ hàng");
        if (dto.branchId() == null)
            throw new BusinessException("Vui lòng chọn chi nhánh");
        Branch branch = branches.lockActive(dto.branchId());
        List<PurchaseLine> lines = purchaseLines(cart, dto);
        // Stable lock ordering reduces the risk of deadlocks across simultaneous
        // checkouts.
        lines = lines.stream().sorted(Comparator.comparing(i -> i.product().getId())).toList();
        BigDecimal subtotal = subtotal(branch.getId(), lines);
        Coupon coupon = coupons.reserve(dto.couponCode(), subtotal);
        BigDecimal discount = coupons.discount(coupon, subtotal);
        BigDecimal shippingFee = shipping.calculate(dto.deliveryMethod(), subtotal);
        Order o = new Order();
        o.setOrderNumber("TD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
        o.setUser(users.byId(userId));
        o.setBranch(branch);
        o.setCoupon(coupon);
        o.setCheckoutToken(blank(dto.checkoutToken())?null:dto.checkoutToken());
        o.setDeliveryMethod(dto.deliveryMethod());
        o.setRecipientName(recipient.name());
        o.setRecipientPhone(recipient.phone());
        o.setShippingAddress(recipient.address());
        o.setNote(dto.note());
        o.setSubtotal(subtotal);
        o.setShippingFee(shippingFee);
        o.setDiscountAmount(discount);
        o.setTotalAmount(subtotal.add(shippingFee).subtract(discount));
        // Reserve stock and write logs in the same transaction as the order.
        for (PurchaseLine line : lines) {
            Product p = line.product();
            inventory.move(branch.getId(), p.getId(), -line.quantity(), StockMovementType.SALE, o.getUser(),
                    o.getOrderNumber(), "Đặt hàng");
            OrderItem item = new OrderItem();
            item.setOrder(o);
            item.setProduct(p);
            item.setProductNameSnapshot(p.getName());
            item.setSkuSnapshot(p.getSku());
            item.setQuantity(line.quantity());
            item.setUnitPrice(p.getPrice());
            item.setSubtotal(p.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            o.getItems().add(item);
        }
        Order saved = orders.saveAndFlush(o);
        Payment payment = new Payment();
        payment.setOrder(saved);
        payment.setAmount(saved.getTotalAmount());
        payment.setMethod(dto.paymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payments.save(payment);
        addHistory(saved, null, OrderStatus.PENDING, o.getUser(), "Khách đặt hàng");
        if (cart != null)
            cartItems.deleteByCartId(cart.getId());
        notifications.create(saved.getUser(), "Đặt hàng thành công", "Đã tiếp nhận đơn " + saved.getOrderNumber(),
                "ORDER");
        notifications.newBranchOrder(branch.getId(), saved.getOrderNumber());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public CheckoutQuoteDto quote(Long userId, CheckoutDto dto) {
        Cart cart = dto.buyNowProductId() == null ? carts.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Giỏ hàng trống")) : null;
        if (cart != null && (cart.getBranch() == null || !cart.getBranch().getId().equals(dto.branchId())))
            throw new BusinessException("Vui lòng chọn chi nhánh trong giỏ hàng");
        if (dto.branchId() == null)
            throw new BusinessException("Vui lòng chọn chi nhánh");
        branches.getActive(dto.branchId());
        if (dto.deliveryMethod() == null)
            throw new BusinessException("Vui lòng chọn hình thức nhận hàng");
        BigDecimal subtotal = subtotal(dto.branchId(), purchaseLines(cart, dto));
        BigDecimal discount = coupons.discount(coupons.preview(dto.couponCode(), subtotal), subtotal);
        BigDecimal fee = shipping.calculate(dto.deliveryMethod(), subtotal);
        return new CheckoutQuoteDto(subtotal, fee, discount, subtotal.add(fee).subtract(discount));
    }

    private List<PurchaseLine> purchaseLines(Cart cart, CheckoutDto dto) {
        if (cart == null) {
            if (dto.buyNowQuantity() == null || dto.buyNowQuantity() < 1 || dto.buyNowQuantity() > 99)
                throw new BusinessException("Số lượng phải từ 1 đến 99");
            return List.of(new PurchaseLine(products.findActive(dto.buyNowProductId()), dto.buyNowQuantity()));
        }
        var lines = cartItems.findByCartIdOrderByIdAsc(cart.getId());
        if (lines.isEmpty())
            throw new BusinessException("Giỏ hàng trống");
        return lines.stream().map(line -> new PurchaseLine(line.getProduct(), line.getQuantity())).toList();
    }

    private record PurchaseLine(Product product, int quantity) {}

    private BigDecimal subtotal(Long branchId, List<PurchaseLine> lines) {
        if (lines.isEmpty())
            throw new BusinessException("Giỏ hàng trống");
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PurchaseLine line : lines) {
            Product p = line.product();
            if (p.getStatus() != ProductStatus.ACTIVE)
                throw new BusinessException("Sản phẩm đã ngừng kinh doanh: " + p.getName());
            if (line.quantity() < 1 || line.quantity() > 99)
                throw new BusinessException("Số lượng không hợp lệ: " + p.getName());
            int available = inventory.available(branchId, p.getId());
            if (available < line.quantity())
                throw new BusinessException(p.getName() + " còn " + available + ", bạn đang chọn " + line.quantity()
                        + ". Vui lòng cập nhật giỏ hàng.");
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        return subtotal;
    }

    private Recipient recipient(Long userId, CheckoutDto dto) {
        if (dto.deliveryMethod() == DeliveryMethod.SHIP_TO_ADDRESS && dto.addressId() != null) {
            var address = addresses.getOwned(userId, dto.addressId());
            return new Recipient(address.recipientName(), address.phone(), address.fullAddress());
        }
        if (blank(dto.recipientName()) || dto.recipientName().trim().length() > 160)
            throw new CheckoutValidationException("recipientName",
                    "Vui lòng nhập họ tên người nhận (tối đa 160 ký tự)");
        if (blank(dto.recipientPhone()) || !dto.recipientPhone().matches("[+0-9][0-9 .()-]{7,29}"))
            throw new CheckoutValidationException("recipientPhone", "Số điện thoại phải có từ 8 đến 30 ký tự hợp lệ");
        if (dto.note() != null && dto.note().length() > 1000)
            throw new CheckoutValidationException("note", "Ghi chú tối đa 1000 ký tự");
        String address = null;
        if (dto.deliveryMethod() == DeliveryMethod.SHIP_TO_ADDRESS) {
            if (!blank(dto.province()) || !blank(dto.district()) || !blank(dto.ward()) || !blank(dto.addressDetail())) {
                if (blank(dto.province()))
                    throw new CheckoutValidationException("province", "Vui lòng nhập tỉnh/thành phố");
                if (blank(dto.district()))
                    throw new CheckoutValidationException("district", "Vui lòng nhập quận/huyện");
                if (blank(dto.ward()))
                    throw new CheckoutValidationException("ward", "Vui lòng nhập phường/xã");
                if (blank(dto.addressDetail()))
                    throw new CheckoutValidationException("addressDetail", "Vui lòng nhập số nhà, tên đường");
                if (dto.province().length() > 120 || dto.district().length() > 120 || dto.ward().length() > 120
                        || dto.addressDetail().length() > 500)
                    throw new CheckoutValidationException("addressDetail", "Địa chỉ quá dài");
                address = dto.addressDetail().trim() + ", " + dto.ward().trim() + ", " + dto.district().trim() + ", "
                        + dto.province().trim();
            } else
                address = dto.shippingAddress();
            if (blank(address) || address.length() > 1000)
                throw new CheckoutValidationException("addressDetail", "Vui lòng nhập địa chỉ giao hàng đầy đủ");
            if (Boolean.TRUE.equals(dto.saveAddress())) {
                if (blank(dto.province()) || blank(dto.district()) || blank(dto.ward()) || blank(dto.addressDetail()))
                    throw new CheckoutValidationException("addressDetail",
                            "Điền địa chỉ có cấu trúc để lưu vào sổ địa chỉ");
                addresses.save(userId, null,
                        new vn.iotstar.dto.address.AddressRequest(dto.recipientName(), dto.recipientPhone(),
                                dto.province(), dto.district(), dto.ward(), dto.addressDetail(), false));
            }
        }
        return new Recipient(dto.recipientName().trim(), dto.recipientPhone().trim(),
                address == null ? null : address.trim());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record Recipient(String name, String phone, String address) {
    }

    @Transactional(readOnly = true)
    public OrderDto branchOrder(Long id, User actor) {
        actor = users.profileById(actor.getId());
        checkOperator(actor);
        Order order = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (actor.getRole().getName() != UserRole.ADMIN
                && (actor.getBranch() == null || !actor.getBranch().getId().equals(order.getBranch().getId())))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không được xem đơn của chi nhánh khác");
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> myOrders(Long userId, Pageable pageable) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> myOrders(Long userId, OrderStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        return filteredOrders(userId, null, status, from, to, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> managedOrders(User actor, OrderStatus status, LocalDate from, LocalDate to,
            Pageable pageable) {
        actor = users.profileById(actor.getId());
        checkOperator(actor);
        if (actor.getRole().getName() != UserRole.ADMIN && actor.getBranch() == null)
            return Page.empty(pageable);
        return filteredOrders(null, actor.getRole().getName() == UserRole.ADMIN ? null : actor.getBranch().getId(),
                status, from, to, pageable).map(this::toDto);
    }

    private Page<Order> filteredOrders(Long userId, Long branchId, OrderStatus status, LocalDate from, LocalDate to,
            Pageable pageable) {
        if (from != null && to != null && from.isAfter(to))
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc");
        var sorted = PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.min(100, pageable.getPageSize()),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return orders.findAll((root, query, cb) -> {
            var clauses = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (userId != null)
                clauses.add(cb.equal(root.get("user").get("id"), userId));
            if (branchId != null)
                clauses.add(cb.equal(root.get("branch").get("id"), branchId));
            if (status != null)
                clauses.add(cb.equal(root.get("status"), status));
            if (from != null)
                clauses.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null)
                clauses.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(clauses.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, sorted);
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrderSummaryDto> customerOrderSummaries(Long userId, String statusFilter,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {
        List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                OrderStatus.READY_FOR_PICKUP);
        Page<Order> result = switch (statusFilter) {
            case "pending" ->
                orders.findByUserIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        userId, activeStatuses, from, to, pageable);
            case "shipping" ->
                orders.findByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        userId, OrderStatus.SHIPPING, from, to, pageable);
            case "done" ->
                orders.findByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        userId, OrderStatus.COMPLETED, from, to, pageable);
            case "cancelled" ->
                orders.findByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        userId, OrderStatus.CANCELLED, from, to, pageable);
            default -> orders.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                    userId, from, to, pageable);
        };
        return result.map(order -> new CustomerOrderSummaryDto(
                order.getId(), order.getOrderNumber(), PROFILE_ORDER_DATE.format(order.getCreatedAt()),
                order.getStatus(), order.getBranch().getName(), order.getItems().size(),
                order.getDeliveryMethod(), order.getTotalAmount()));
    }

    @Transactional(readOnly = true)
    public OrderDto myOrder(Long userId, Long id) {
        return toDto(orders.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng")));
    }

    @Transactional(readOnly = true)
    public OrderDto myOrderByNumber(Long userId, String orderNumber) {
        return toDto(orders.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng")));
    }

    @Transactional(readOnly = true)
    public OrderDetailDto myOrderDetail(Long userId, Long id) {
        Order order = orders.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return detail(order, null);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto branchOrderDetail(Long id, User actor) {
        actor = users.profileById(actor.getId());
        Order order = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        checkScope(order, actor);
        return detail(order, actor);
    }

    @Transactional
    public void cancelByCustomer(Long userId, Long id) {
        Order o = orders.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (!o.getUser().getId().equals(userId))
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng");
        if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.CONFIRMED)
            throw new BusinessException("Đơn hàng không thể hủy ở trạng thái hiện tại");
        if (payments.findByOrderId(id).stream().anyMatch(p -> p.getStatus() == PaymentStatus.PAID))
            throw new BusinessException("Đơn đã ghi nhận thanh toán; cần quy trình hoàn tiền thủ công");
        coupons.restore(o.getCoupon());
        for (OrderItem line : o.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).toList())
            inventory.move(o.getBranch().getId(), line.getProduct().getId(), line.getQuantity(),
                    StockMovementType.RETURN,
                    o.getUser(), o.getOrderNumber(), "Hoàn kho khi hủy đơn");
        OrderStatus previous = o.getStatus();
        o.setStatus(OrderStatus.CANCELLED);
        addHistory(o, previous, OrderStatus.CANCELLED, o.getUser(), "Khách hủy đơn");
        notifications.create(o.getUser(), "Đã hủy đơn", "Đơn " + o.getOrderNumber() + " đã hủy", "ORDER");
    }

    @Transactional
    public void advanceStatus(Long id, OrderStatus next, User actor) {
        advanceStatus(id, next, actor, false, null);
    }

    @Transactional
    public void advanceStatus(Long id, OrderStatus next, User actor, boolean codCollected, String note) {
        Order o = orders.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
        actor = users.profileById(actor.getId());
        checkScope(o, actor);
        OrderStatus before = o.getStatus();
        if (!nextStatuses(o, actor).contains(next))
            throw new BusinessException("Không thể chuyển từ " + before.getLabel() + " sang "
                    + (next == null ? "trạng thái trống" : next.getLabel()));
        if (note != null && note.length() > 500)
            throw new BusinessException("Ghi chú xử lý tối đa 500 ký tự");
        if ((next == OrderStatus.DELIVERY_FAILED || next == OrderStatus.RETURNING || next == OrderStatus.RETURNED)
                && blank(note))
            throw new BusinessException("Vui lòng ghi lý do/xác nhận bàn giao hàng");
        if (next == OrderStatus.COMPLETED) {
            var paymentRows=payments.findByOrderId(id);
            if(paymentRows.isEmpty())throw new BusinessException("Đơn chưa có thông tin thanh toán để đối soát");
            for (Payment payment : paymentRows) {
                if (payment.getMethod() == PaymentMethod.CASH_ON_DELIVERY
                        && payment.getStatus() == PaymentStatus.PENDING) {
                    if (!codCollected)
                        throw new BusinessException("Phải xác nhận đã thu đủ tiền COD trước khi hoàn tất đơn");
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setConfirmedBy(actor);
                    payment.setTransactionId("COD-" + UUID.randomUUID());
                }
                if(payment.getStatus()!=PaymentStatus.PAID)throw new BusinessException("Phải đối soát khoản thanh toán hợp lệ trước khi hoàn tất đơn");
            }
            if(paymentRows.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(o.getTotalAmount())!=0)
                throw new BusinessException("Tổng tiền thu chưa khớp với đơn hàng; cần kiểm tra thanh toán");
            o.setCompletedAt(LocalDateTime.now());
        }
        if (next == OrderStatus.RETURNED) {
            coupons.restore(o.getCoupon());
            restoreStock(o, actor, "Đã nhận hàng hoàn: " + note);
        }
        o.setStatus(next);
        addHistory(o, before, next, actor, blank(note) ? "Cập nhật trạng thái" : note.trim());
        notifications.create(o.getUser(), "Cập nhật đơn hàng", o.getOrderNumber() + " · " + next.getLabel(), "ORDER");
    }

    @Transactional
    public void confirmRefund(Long id, User actor, String note) {
        Order order = orders.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        actor = users.profileById(actor.getId());
        checkScope(order, actor);
        if (actor.getRole().getName() == UserRole.STAFF)
            throw new org.springframework.security.access.AccessDeniedException("Chỉ quản lý được xác nhận hoàn tiền");
        if (order.getStatus() != OrderStatus.RETURNED || blank(note) || note.length() > 500)
            throw new BusinessException("Chỉ xác nhận hoàn tiền sau khi nhận hàng hoàn và có ghi chú đối soát");
        var paid = payments.findByOrderId(id).stream().filter(p -> p.getStatus() == PaymentStatus.PAID).toList();
        if (paid.isEmpty())
            throw new BusinessException("Không có khoản tiền cần hoàn");
        for (Payment payment : paid) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
        }
        addHistory(order, OrderStatus.RETURNED, OrderStatus.RETURNED, actor, "Đã xác nhận hoàn tiền: " + note.trim());
        notifications.create(order.getUser(), "Đã hoàn tiền", order.getOrderNumber() + " đã được xác nhận hoàn tiền",
                "ORDER");
    }

    private void checkOperator(User actor) {
        if (actor.getStatus() != UserStatus.ACTIVE || actor.getRole().getName() == UserRole.CUSTOMER)
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền xử lý đơn hàng");
        if (actor.getRole().getName() != UserRole.ADMIN && actor.getBranch() != null
                && actor.getBranch().getStatus() != BranchStatus.ACTIVE)
            throw new org.springframework.security.access.AccessDeniedException("Chi nhánh đã ngừng hoạt động");
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> adminOrdersAtBranch(Long branchId, User actor, Pageable pageable) {
        actor = users.profileById(actor.getId());
        checkOperator(actor);
        if (actor.getRole().getName() != UserRole.ADMIN)
            throw new org.springframework.security.access.AccessDeniedException("Chỉ quản trị viên được xem chi nhánh khác");
        return filteredOrders(null, branchId, null, null, null, pageable).map(this::toDto);
    }

    private void checkScope(Order order, User actor) {
        checkOperator(actor);
        if (actor.getRole().getName() != UserRole.ADMIN
                && (actor.getBranch() == null || !actor.getBranch().getId().equals(order.getBranch().getId())))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không được xử lý đơn của chi nhánh khác");
    }

    private List<OrderStatus> nextStatuses(Order order, User actor) {
        if (actor == null)
            return List.of();
        return switch (order.getStatus()) {
            case PENDING -> List.of(OrderStatus.CONFIRMED);
            case CONFIRMED -> List.of(OrderStatus.PREPARING);
            case PREPARING ->
                List.of(order.getDeliveryMethod() == DeliveryMethod.PICKUP_AT_BRANCH ? OrderStatus.READY_FOR_PICKUP
                        : OrderStatus.SHIPPING);
            case READY_FOR_PICKUP -> List.of(OrderStatus.COMPLETED);
            case SHIPPING -> List.of(OrderStatus.COMPLETED, OrderStatus.DELIVERY_FAILED);
            case DELIVERY_FAILED -> List.of(OrderStatus.SHIPPING, OrderStatus.RETURNING);
            case RETURNING -> List.of(OrderStatus.RETURNED);
            case COMPLETED -> actor.getRole().getName() == UserRole.STAFF ? List.of() : List.of(OrderStatus.RETURNING);
            default -> List.of();
        };
    }

    private void restoreStock(Order order, User actor, String reason) {
        for (OrderItem line : order.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList())
            inventory.move(order.getBranch().getId(), line.getProduct().getId(), line.getQuantity(),
                    StockMovementType.RETURN, actor, order.getOrderNumber(), reason);
    }

    private OrderDetailDto detail(Order order, User actor) {
        var paymentList = payments.findByOrderId(order.getId());
        Payment payment = paymentList.stream().sorted(Comparator.comparing(p -> p.getStatus() != PaymentStatus.PAID))
                .findFirst().orElse(null);
        var lines = order.getItems().stream()
                .map(item -> new OrderDetailItemDto(item.getProduct().getId(), item.getProductNameSnapshot(),
                        item.getSkuSnapshot(),
                        primaryImage(item.getProduct()), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()))
                .toList();
        var timeline = histories.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(h -> new OrderHistoryDto(h.getPreviousStatus(), h.getNewStatus(),
                        formatDate(h.getCreatedAt()),
                        actor==null?null:(h.getChangedBy() == null ? "Hệ thống" : h.getChangedBy().getFullName()),
                        actor==null?(h.getPreviousStatus()==OrderStatus.RETURNED&&h.getNewStatus()==OrderStatus.RETURNED?"Đã xác nhận hoàn tiền":null):h.getNote()))
                .toList();
        boolean pendingCod = paymentList.stream().anyMatch(
                p -> p.getMethod() == PaymentMethod.CASH_ON_DELIVERY && p.getStatus() == PaymentStatus.PENDING);
        boolean canCancel = (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED)
                && paymentList.stream().noneMatch(p -> p.getStatus() == PaymentStatus.PAID);
        boolean refund = actor != null && actor.getRole().getName() != UserRole.STAFF
                && order.getStatus() == OrderStatus.RETURNED
                && paymentList.stream().anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
        return new OrderDetailDto(order.getId(), order.getOrderNumber(), order.getStatus(),
                formatDate(order.getCreatedAt()), formatDate(order.getCompletedAt()),
                order.getBranch().getId(), order.getBranch().getName(), order.getBranch().getAddress(),
                order.getDeliveryMethod(),
                order.getRecipientName(), order.getRecipientPhone(), order.getShippingAddress(), order.getNote(),
                order.getSubtotal(), order.getShippingFee(),
                order.getDiscountAmount(), order.getTotalAmount(), lines, payment == null ? null : payment.getMethod(),
                payment == null ? null : payment.getStatus(), timeline,
                nextStatuses(order, actor), canCancel, pendingCod, refund,
                payment==null?null:formatDate(payment.getPaidAt()),
                payment==null||payment.getConfirmedBy()==null?null:payment.getConfirmedBy().getFullName(),
                payment==null?null:formatDate(payment.getRefundedAt()));
    }

    private String primaryImage(Product product) {
        return product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isPrimary).reversed()
                        .thenComparingInt(ProductImage::getSortOrder))
                .map(ProductImage::getImageUrl).findFirst().orElse(null);
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : PROFILE_ORDER_DATE.format(value);
    }

    @Transactional(readOnly = true)
    public OrderDto toDto(Order o) {
        List<OrderItemDto> lines = o.getItems().stream()
                .map(i -> new OrderItemDto(i.getProduct().getId(), i.getProductNameSnapshot(),
                        i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                .toList();
        return new OrderDto(o.getId(), o.getOrderNumber(), o.getStatus().name(), o.getTotalAmount(),
                o.getBranch().getId(), lines);
    }

    private void addHistory(Order order, OrderStatus previous, OrderStatus next, User actor, String note) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setPreviousStatus(previous);
        h.setNewStatus(next);
        h.setChangedBy(actor);
        h.setNote(note);
        histories.save(h);
    }
}
