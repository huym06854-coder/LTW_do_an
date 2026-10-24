package vn.iotstar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;
import vn.iotstar.domain.entity.Branch;
import vn.iotstar.domain.entity.BranchStock;
import vn.iotstar.domain.entity.Cart;
import vn.iotstar.domain.entity.CartItem;
import vn.iotstar.domain.entity.Category;
import vn.iotstar.domain.entity.Order;
import vn.iotstar.domain.entity.Payment;
import vn.iotstar.domain.entity.Product;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.DeliveryMethod;
import vn.iotstar.domain.enums.OrderStatus;
import vn.iotstar.domain.enums.PaymentMethod;
import vn.iotstar.domain.enums.PaymentStatus;
import vn.iotstar.domain.enums.StockMovementType;
import vn.iotstar.domain.enums.UserRole;
import vn.iotstar.dto.address.AddressRequest;
import vn.iotstar.dto.order.CheckoutDto;
import vn.iotstar.dto.order.OrderDto;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.exception.CheckoutValidationException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.BranchRepository;
import vn.iotstar.repository.BranchStockRepository;
import vn.iotstar.repository.CartItemRepository;
import vn.iotstar.repository.CartRepository;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.OrderRepository;
import vn.iotstar.repository.OrderItemRepository;
import vn.iotstar.repository.OrderStatusHistoryRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.StockLogRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.CustomerUserDetails;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.locations=classpath:db/migration/h2",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.shipping.flat-fee=30000", "app.shipping.free-from=5000000",
        "ADMIN_BOOTSTRAP_USERNAME=", "ADMIN_BOOTSTRAP_EMAIL=", "ADMIN_BOOTSTRAP_PASSWORD=", "ADMIN_BOOTSTRAP_FULL_NAME=",
        "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD=", "ADMIN_FULL_NAME="
})
@AutoConfigureMockMvc
class OrderWorkflowIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private OrderService workflow;
    @Autowired private InventoryService inventory;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CartService cartService;
    @Autowired private AddressService addresses;
    @Autowired private BranchRepository branches;
    @Autowired private BranchStockRepository stocks;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private CartRepository carts;
    @Autowired private CartItemRepository cartItems;
    @Autowired private OrderRepository orders;
    @Autowired private OrderItemRepository orderItems;
    @Autowired private PaymentRepository payments;
    @Autowired private StockLogRepository logs;
    @Autowired private OrderStatusHistoryRepository histories;

    @Test
    void checkoutShowsSavedAddressesAndTheAuthoritativeShippingAndTotalAmounts() throws Exception {
        Fixture f = fixture(10, 2);
        var address = addresses.save(f.customer().getId(), null, new AddressRequest(
                "Địa chỉ mặc định", "0901234567", "TP. Hồ Chí Minh", "Quận 1", "Phường 1", "25 Nguyễn Trãi", true));
        var expectedQuote = workflow.quote(f.customer().getId(), checkout(f, DeliveryMethod.SHIP_TO_ADDRESS, address.id(), token()));
        assertThat(expectedQuote.shippingFee()).isEqualByComparingTo("30000");
        mvc.perform(get("/checkout").with(user(new CustomerUserDetails(f.customer()))))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/checkout"))
                .andExpect(model().attribute("quote", expectedQuote))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"addressId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Địa chỉ mặc định")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(address.fullAddress())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("30.000₫")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2.430.000₫")));
    }

    @Test
    void checkoutRedirectsToCartUntilABranchHasBeenSelected() throws Exception {
        Fixture f = fixture(10, 2);
        User shopper = customer();
        cart(shopper, null, f.product(), 2);
        mvc.perform(get("/checkout").with(user(new CustomerUserDetails(shopper))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
        assertThat(orderCount(shopper)).isZero();
    }

    @Test
    void buyNowOrdersOnlySelectedQuantityAndLeavesCartForLaterCheckout() throws Exception {
        Fixture f = fixture(10, 2);
        var principal = user(new CustomerUserDetails(f.customer()));
        mvc.perform(post("/checkout/buy-now").with(principal).with(csrf())
                        .param("productId", f.product().getId().toString()).param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/checkout/buy-now").with(principal)
                        .param("productId", f.product().getId().toString()).param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("buyNow", true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"buyNowProductId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm đặt hàng")));
        mvc.perform(post("/checkout/branch").with(principal).with(csrf())
                        .param("branchId", f.branch().getId().toString())
                        .param("buyNowProductId", f.product().getId().toString())
                        .param("buyNowQuantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutAllowed").value(true));
        assertThat(cartService.view(f.customer().getId()).branchId()).isEqualTo(f.branch().getId());

        var direct = new CheckoutDto(f.branch().getId(), "Khách hàng", "0901234567", null,
                DeliveryMethod.PICKUP_AT_BRANCH, PaymentMethod.CASH_ON_DELIVERY, null, null,
                null, false, null, null, null, null, token(), f.product().getId(), 1);
        assertThat(workflow.quote(f.customer().getId(), direct).subtotal()).isEqualByComparingTo("1200000");
        mvc.perform(post("/checkout/place-order").with(principal).with(csrf())
                        .param("branchId", f.branch().getId().toString())
                        .param("recipientName", "Khách hàng").param("recipientPhone", "0901234567")
                        .param("deliveryMethod", "PICKUP_AT_BRANCH")
                        .param("paymentMethod", "CASH_ON_DELIVERY")
                        .param("checkoutToken", direct.checkoutToken())
                        .param("buyNowProductId", f.product().getId().toString())
                        .param("buyNowQuantity", "1"))
                .andExpect(status().is3xxRedirection());
        Order purchased = orders.findByUserIdOrderByCreatedAtDesc(f.customer().getId(), Pageable.unpaged())
                .getContent().get(0);
        assertThat(orderItems.findByOrderId(purchased.getId())).hasSize(1)
                .first().extracting(item -> item.getQuantity()).isEqualTo(1);
        assertThat(stock(f)).isEqualTo(9);
        assertThat(cartItems.findByCartIdOrderByIdAsc(f.cart().getId()).get(0).getQuantity()).isEqualTo(2);

        OrderDto cartOrder = workflow.checkout(f.customer().getId(),
                checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token()));
        assertThat(cartOrder.items().get(0).quantity()).isEqualTo(2);
        assertThat(stock(f)).isEqualTo(7);
        assertThat(cartItems.findByCartIdOrderByIdAsc(f.cart().getId())).isEmpty();
    }

    @Test
    void invalidManualShippingFormKeepsUserInputAndShowsFieldErrorsWithoutCreatingAnOrder() throws Exception {
        Fixture f = fixture(10, 2);
        mvc.perform(post("/checkout/place-order").with(user(new CustomerUserDetails(f.customer()))).with(csrf())
                        .param("branchId", f.branch().getId().toString())
                        .param("recipientName", "Nguyễn Người Đặt Giữ Lại").param("recipientPhone", "invalid")
                        .param("shippingAddress", "").param("deliveryMethod", "SHIP_TO_ADDRESS")
                        .param("paymentMethod", "CASH_ON_DELIVERY").param("couponCode", "")
                        .param("note", "Giao sau 17 giờ").param("addressId", "").param("saveAddress", "false")
                        .param("province", "").param("district", "Quận 1").param("ward", "")
                        .param("addressDetail", "25 Nguyễn Trãi").param("checkoutToken", token()))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/checkout"))
                .andExpect(model().attributeHasFieldErrors("checkout", "recipientPhone", "province", "ward"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Người Đặt Giữ Lại")))
                .andExpect(response -> assertThat(HtmlUtils.htmlUnescape(response.getResponse().getContentAsString()))
                        .containsPattern("<input\\b(?=[^>]*\\bname=\"addressDetail\")(?=[^>]*\\bvalue=\"25 Nguyễn Trãi\")[^>]*>"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Giao sau 17 giờ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng kiểm tra thông tin đặt hàng")));
        assertThat(orderCount(f.customer())).isZero();
        assertThat(stock(f)).isEqualTo(10);
        assertThat(cartItems.findByCartIdOrderByIdAsc(f.cart().getId())).hasSize(1);
    }

    @Test
    void cartOnlyEnablesCheckoutAfterTheCustomerSelectsAnAvailableBranch() throws Exception {
        Fixture f = fixture(10, 2);
        User shopper = customer();
        cart(shopper, null, f.product(), 2);
        var principal = user(new CustomerUserDetails(shopper));

        mvc.perform(get("/cart").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/checkout\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hãy chọn chi nhánh")));

        cartService.chooseBranch(shopper.getId(), f.branch().getId());
        mvc.perform(get("/cart").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/checkout\"")));
    }

    @Test
    void ajaxQuantityUpdateReturnsStockErrorAndTheUnchangedCart() throws Exception {
        Fixture f = fixture(3, 2);
        mvc.perform(post("/cart/quantity").with(user(new CustomerUserDetails(f.customer()))).with(csrf())
                        .param("productId", f.product().getId().toString()).param("quantity", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error", org.hamcrest.Matchers.containsString(f.product().getName())))
                .andExpect(jsonPath("$.cart.items[0].quantity").value(2))
                .andExpect(jsonPath("$.cart.items[0].availableStock").value(3));
        assertThat(cartItems.findByCartIdOrderByIdAsc(f.cart().getId()).get(0).getQuantity()).isEqualTo(2);
        assertThat(stock(f)).isEqualTo(3);
    }

    @Test
    void changingBranchListsAllInsufficientProductsAndKeepsThePreviousBranch() {
        Fixture f = fixture(10, 3);
        Product second = new Product();
        second.setSku("WF-" + UUID.randomUUID());
        second.setName("Phụ kiện thứ hai");
        second.setPrice(new BigDecimal("200000.00"));
        second.setCategory(f.product().getCategory());
        second = products.saveAndFlush(second);
        CartItem extra = new CartItem();
        extra.setCart(f.cart());
        extra.setProduct(second);
        extra.setQuantity(2);
        cartItems.saveAndFlush(extra);
        Branch alternative = new Branch();
        alternative.setCode("ALT-" + token().substring(0, 20));
        alternative.setName("Chi nhánh thiếu hàng");
        alternative.setAddress("Địa chỉ chi nhánh khác");
        alternative = branches.saveAndFlush(alternative);
        BranchStock remaining = new BranchStock();
        remaining.setBranch(alternative);
        remaining.setProduct(f.product());
        remaining.setQuantity(1);
        stocks.saveAndFlush(remaining);
        Long alternativeId = alternative.getId();

        assertThatThrownBy(() -> cartService.chooseBranch(f.customer().getId(), alternativeId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(f.product().getName()).hasMessageContaining("Phụ kiện thứ hai")
                .hasMessageContaining("còn 1").hasMessageContaining("còn 0");
        assertThat(cartService.view(f.customer().getId()).branchId()).isEqualTo(f.branch().getId());
    }

    @Test
    void savedAddressIsCopiedToOrderAndSurvivesLaterChangesAndDeletion() {
        Fixture f = fixture(10, 2);
        var address = addresses.save(f.customer().getId(), null, new AddressRequest(
                "Người nhận ban đầu", "0901234567", "TP. Hồ Chí Minh", "Quận 1", "Phường 1", "25 Nguyễn Trãi", true));
        CheckoutDto request = checkout(f, DeliveryMethod.SHIP_TO_ADDRESS, address.id(), token());
        var quote = workflow.quote(f.customer().getId(), request);
        Long id = workflow.checkout(f.customer().getId(), request).id();

        addresses.save(f.customer().getId(), address.id(), new AddressRequest(
                "Người nhận mới", "0912345678", "Hà Nội", "Quận Ba Đình", "Phường 2", "120 Đường Mới", true));
        addresses.delete(f.customer().getId(), address.id());

        Order saved = order(id);
        assertThat(saved.getRecipientName()).isEqualTo("Người nhận ban đầu");
        assertThat(saved.getRecipientPhone()).isEqualTo("0901234567");
        assertThat(saved.getShippingAddress()).isEqualTo(address.fullAddress());
        assertThat(saved.getShippingFee()).isEqualByComparingTo(quote.shippingFee());
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(quote.totalAmount());
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(saved.getSubtotal().add(saved.getShippingFee()).subtract(saved.getDiscountAmount()));
    }

    @Test
    void checkoutCannotSelectAnAddressOwnedByAnotherCustomer() {
        Fixture f = fixture(10, 2);
        User other = customer();
        var address = addresses.save(other.getId(), null, new AddressRequest(
                "Khách hàng khác", "0901234567", "TP. Hồ Chí Minh", "Quận 1", "Phường 1", "25 Nguyễn Trãi", true));

        assertThatThrownBy(() -> workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.SHIP_TO_ADDRESS, address.id(), token())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(orderCount(f.customer())).isZero();
        assertThat(stock(f)).isEqualTo(10);
    }

    @Test
    void deliveryRequiresAnAddressButPickupDoesNot() {
        Fixture f = fixture(10, 2);
        assertThatThrownBy(() -> workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.SHIP_TO_ADDRESS, null, token())))
                .isInstanceOf(CheckoutValidationException.class);
        assertThat(orderCount(f.customer())).isZero();
        assertThat(stock(f)).isEqualTo(10);

        Long pickup = workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token())).id();
        assertThat(order(pickup).getShippingAddress()).isNull();
        assertThat(order(pickup).getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stock(f)).isEqualTo(8);
    }

    @Test
    void retryingCheckoutReturnsSameOrderAfterTheCartWasCleared() {
        Fixture f = fixture(10, 2);
        CheckoutDto request = checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token());
        var first = workflow.checkout(f.customer().getId(), request);
        assertThat(cartItems.findByCartIdOrderByIdAsc(f.cart().getId())).isEmpty();
        var retried = workflow.checkout(f.customer().getId(), request);

        assertThat(retried.id()).isEqualTo(first.id());
        assertThat(orderCount(f.customer())).isEqualTo(1);
        assertThat(stock(f)).isEqualTo(8);
        assertThat(payments.findByOrderId(first.id())).hasSize(1);
        assertThat(movements(f)).hasSize(1);
    }

    @Test
    void concurrentRequestsWithSameCheckoutTokenCreateOnlyOneOrderAndStockMovement() throws Exception {
        Fixture f = fixture(10, 3);
        CheckoutDto request = checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token());
        List<OrderDto> results = concurrently(
                () -> workflow.checkout(f.customer().getId(), request),
                () -> workflow.checkout(f.customer().getId(), request));

        assertThat(results.get(0).id()).isEqualTo(results.get(1).id());
        assertThat(orderCount(f.customer())).isEqualTo(1);
        assertThat(stock(f)).isEqualTo(7);
        assertThat(movements(f)).hasSize(1);
        assertThat(movements(f).get(0).getQuantityChange()).isEqualTo(-3);
    }

    @Test
    void differentCustomersCompetingForLastItemsCannotOversell() throws Exception {
        Fixture f = fixture(5, 3);
        User other = customer();
        Cart otherCart = cart(other, f.branch(), f.product(), 3);
        Fixture second = new Fixture(other, f.actor(), f.branch(), f.product(), otherCart);
        List<Boolean> results = concurrently(
                () -> tryCheckout(f),
                () -> tryCheckout(second));

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(orderCount(f.customer()) + orderCount(other)).isEqualTo(1);
        assertThat(stock(f)).isEqualTo(2);
        assertThat(movements(f)).hasSize(1);
    }

    @Test
    void cachedStockIsRefreshedAfterAnotherCommittedPurchaseBeforeCheckingAvailabilityForSale() {
        Fixture f = fixture(5, 3);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try (var buyer = Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                // Keep version/quantity 5 in this transaction's first-level cache.
                assertThat(inventory.available(f.branch().getId(), f.product().getId())).isEqualTo(5);
                var committedPurchase = buyer.submit(() -> workflow.checkout(f.customer().getId(),
                        checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token())));
                try {
                    committedPurchase.get(20, TimeUnit.SECONDS);
                } catch (Exception error) {
                    throw new AssertionError("The competing purchase must commit before this stale transaction resumes", error);
                }
                // Three more units would exceed the latest quantity 2, even though this transaction cached 5.
                inventory.move(f.branch().getId(), f.product().getId(), -3, StockMovementType.SALE,
                        f.actor(), "STALE-CACHE-ATTEMPT", "Kiểm tra tồn kho sau giao dịch đồng thời");
            })).isInstanceOf(BusinessException.class);
        }
        assertThat(stock(f)).isEqualTo(2);
        assertThat(orderCount(f.customer())).isEqualTo(1);
        assertThat(movements(f)).hasSize(1);
        assertThat(movements(f).get(0).getQuantityChange()).isEqualTo(-3);
        assertThat(movements(f).get(0).getReferenceCode()).isNotEqualTo("STALE-CACHE-ATTEMPT");
    }

    @Test
    void insufficientStockLeavesCartOrderAndInventoryUnchanged() {
        Fixture f = fixture(2, 3);
        assertThatThrownBy(() -> workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token())))
                .isInstanceOf(BusinessException.class);
        assertThat(orderCount(f.customer())).isZero();
        assertThat(stock(f)).isEqualTo(2);
        assertThat(cartItems.findByCartIdOrderByIdAsc(f.cart().getId())).hasSize(1);
        assertThat(movements(f)).isEmpty();
    }

    @Test
    void cancellingSameOrderTwiceRestoresStockOnlyOnce() {
        Fixture f = fixture(10, 3);
        Long id = workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token())).id();
        workflow.cancelByCustomer(f.customer().getId(), id);
        allowRejectedDuplicate(() -> workflow.cancelByCustomer(f.customer().getId(), id));

        assertThat(order(id).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stock(f)).isEqualTo(10);
        assertThat(movements(f).stream().filter(log -> log.getType() == StockMovementType.RETURN)).hasSize(1);
        assertThat(histories.findByOrderIdOrderByCreatedAtAsc(id).stream().filter(h -> h.getNewStatus() == OrderStatus.CANCELLED)).hasSize(1);
    }

    @Test
    void pickupBecomesReadyBeforeHandoverAndRequiresExplicitCodCollection() {
        Fixture f = fixture(10, 2);
        Long id = workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token())).id();
        assertThatThrownBy(() -> advance(f, id, OrderStatus.READY_FOR_PICKUP, false)).isInstanceOf(BusinessException.class);
        assertThat(order(id).getStatus()).isEqualTo(OrderStatus.PENDING);
        advance(f, id, OrderStatus.CONFIRMED, false);
        advance(f, id, OrderStatus.PREPARING, false);
        assertThatThrownBy(() -> advance(f, id, OrderStatus.COMPLETED, true)).isInstanceOf(BusinessException.class);
        advance(f, id, OrderStatus.READY_FOR_PICKUP, false);
        assertThatThrownBy(() -> advance(f, id, OrderStatus.COMPLETED, false)).isInstanceOf(BusinessException.class);
        assertThat(order(id).getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
        assertThat(payment(id).getStatus()).isEqualTo(PaymentStatus.PENDING);

        advance(f, id, OrderStatus.COMPLETED, true);
        assertThat(order(id).getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order(id).getCompletedAt()).isNotNull();
        assertThat(payment(id).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment(id).getPaidAt()).isNotNull();
        assertThat(payment(id).getConfirmedBy().getId()).isEqualTo(f.actor().getId());
        assertThat(stock(f)).isEqualTo(8);
    }

    @Test
    void failedDeliveryRestocksOnlyAfterTheGoodsAreConfirmedReturned() {
        Fixture f = fixture(10, 3);
        CheckoutDto request = new CheckoutDto(f.branch().getId(), "Khách hàng", "0901234567", "25 Nguyễn Trãi, TP. Hồ Chí Minh",
                DeliveryMethod.SHIP_TO_ADDRESS, PaymentMethod.CASH_ON_DELIVERY, null, null);
        Long id = workflow.checkout(f.customer().getId(), request).id();
        advance(f, id, OrderStatus.CONFIRMED, false);
        advance(f, id, OrderStatus.PREPARING, false);
        advance(f, id, OrderStatus.SHIPPING, false);
        advance(f, id, OrderStatus.DELIVERY_FAILED, false);
        assertThat(stock(f)).isEqualTo(7);
        assertThat(payment(id).getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThatThrownBy(() -> workflow.cancelByCustomer(f.customer().getId(), id)).isInstanceOf(BusinessException.class);
        assertThat(stock(f)).isEqualTo(7);
        advance(f, id, OrderStatus.RETURNING, false);
        assertThat(stock(f)).isEqualTo(7);
        advance(f, id, OrderStatus.RETURNED, false);
        allowRejectedDuplicate(() -> advance(f, id, OrderStatus.RETURNED, false));

        assertThat(order(id).getStatus()).isEqualTo(OrderStatus.RETURNED);
        assertThat(stock(f)).isEqualTo(10);
        assertThat(movements(f).stream().filter(log -> log.getType() == StockMovementType.RETURN)).hasSize(1);
    }

    @Test
    void customerCannotReadOrCancelAnotherCustomersOrder() {
        Fixture f = fixture(10, 2);
        User other = customer();
        var placed = workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token()));

        assertThatThrownBy(() -> workflow.myOrder(other.getId(), placed.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> workflow.myOrderByNumber(other.getId(), placed.orderNumber())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> workflow.cancelByCustomer(other.getId(), placed.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(order(placed.id()).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(stock(f)).isEqualTo(8);
    }

    @Test
    void paidReturnNeedsManagerApprovalAndRefundIsConfirmedOnlyOnceAfterPhysicalReturn() {
        Fixture f = fixture(10, 2);
        Long id = workflow.checkout(f.customer().getId(), checkout(f, DeliveryMethod.PICKUP_AT_BRANCH, null, token())).id();
        advance(f, id, OrderStatus.CONFIRMED, false);
        advance(f, id, OrderStatus.PREPARING, false);
        advance(f, id, OrderStatus.READY_FOR_PICKUP, false);
        advance(f, id, OrderStatus.COMPLETED, true);
        User manager = account(UserRole.MANAGER, f.branch());

        assertThatThrownBy(() -> workflow.advanceStatus(id, OrderStatus.RETURNING, f.actor(), false, "Khách yêu cầu trả hàng"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> workflow.confirmRefund(id, manager, "Chưa nhận lại hàng"))
                .isInstanceOf(BusinessException.class);
        assertThat(payment(id).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(stock(f)).isEqualTo(8);

        workflow.advanceStatus(id, OrderStatus.RETURNING, manager, false, "Duyệt yêu cầu trả hàng");
        assertThat(payment(id).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(stock(f)).isEqualTo(8);
        workflow.advanceStatus(id, OrderStatus.RETURNED, manager, false, "Đã nhận và kiểm tra đủ hàng tại kho");
        assertThat(stock(f)).isEqualTo(10);
        assertThat(payment(id).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(workflow.branchOrderDetail(id, manager).canConfirmRefund()).isTrue();
        assertThat(workflow.branchOrderDetail(id, f.actor()).canConfirmRefund()).isFalse();

        assertThatThrownBy(() -> workflow.confirmRefund(id, f.actor(), "Nhân viên thử xác nhận hoàn tiền"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> workflow.confirmRefund(id, manager, " "))
                .isInstanceOf(BusinessException.class);
        workflow.confirmRefund(id, manager, "Đã đối soát hoàn đủ tiền cho khách hàng");
        assertThat(payment(id).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment(id).getRefundedAt()).isNotNull();
        assertThat(workflow.branchOrderDetail(id, manager).canConfirmRefund()).isFalse();
        assertThatThrownBy(() -> workflow.confirmRefund(id, manager, "Gửi xác nhận hoàn tiền lặp"))
                .isInstanceOf(BusinessException.class);
        assertThat(stock(f)).isEqualTo(10);
        assertThat(movements(f).stream().filter(log -> log.getType() == StockMovementType.RETURN)).hasSize(1);
    }

    private void advance(Fixture f, Long id, OrderStatus status, boolean collected) {
        workflow.advanceStatus(id, status, f.actor(), collected, "Xác nhận bởi nhân viên kiểm thử");
    }

    private boolean tryCheckout(Fixture fixture) {
        try {
            workflow.checkout(fixture.customer().getId(), checkout(fixture, DeliveryMethod.PICKUP_AT_BRANCH, null, token()));
            return true;
        } catch (BusinessException expected) {
            return false;
        }
    }

    private void allowRejectedDuplicate(Runnable action) {
        try { action.run(); } catch (BusinessException expected) { /* Rejection and idempotent success both preserve the invariant. */ }
    }

    private <T> List<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var a = workers.submit(() -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return first.call(); });
            var b = workers.submit(() -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return second.call(); });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
    }

    private Fixture fixture(int available, int requested) {
        String suffix = UUID.randomUUID().toString();
        Category category = new Category();
        category.setName("Workflow " + suffix);
        category = categories.saveAndFlush(category);
        Product product = new Product();
        product.setSku("WF-" + suffix);
        product.setName("Thiết bị kiểm thử " + suffix);
        product.setPrice(new BigDecimal("1200000.00"));
        product.setCategory(category);
        product = products.saveAndFlush(product);
        Branch branch = new Branch();
        branch.setCode("WF-" + suffix.substring(0, 20));
        branch.setName("Chi nhánh kiểm thử " + suffix);
        branch.setAddress("120 Nguyễn Văn Thủ, TP. Hồ Chí Minh");
        branch = branches.saveAndFlush(branch);
        BranchStock stock = new BranchStock();
        stock.setBranch(branch);
        stock.setProduct(product);
        stock.setQuantity(available);
        stocks.saveAndFlush(stock);
        User customer = customer();
        User actor = account(UserRole.STAFF, branch);
        return new Fixture(customer, actor, branch, product, cart(customer, branch, product, requested));
    }

    private Cart cart(User owner, Branch branch, Product product, int requested) {
        Cart cart = new Cart();
        cart.setUser(owner);
        cart.setBranch(branch);
        cart = carts.saveAndFlush(cart);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(requested);
        cartItems.saveAndFlush(item);
        return cart;
    }

    private User customer() { return account(UserRole.CUSTOMER, null); }

    private User account(UserRole role, Branch branch) {
        String suffix = UUID.randomUUID().toString();
        User account = new User();
        account.setFullName("Khách hàng kiểm thử");
        account.setUsername("wf-" + suffix);
        account.setEmail("wf-" + suffix + "@example.com");
        account.setPassword("integration-test-unused-password");
        account.setPhone("0901234567");
        account.setRole(roles.findByName(role).orElseThrow());
        account.setBranch(branch);
        return users.saveAndFlush(account);
    }

    private CheckoutDto checkout(Fixture f, DeliveryMethod delivery, Long addressId, String token) {
        return new CheckoutDto(f.branch().getId(), "Khách hàng", "0901234567", null,
                delivery, PaymentMethod.CASH_ON_DELIVERY, null, null, addressId, false,
                null, null, null, null, token);
    }

    private String token() { return UUID.randomUUID().toString(); }
    private Order order(Long id) { return orders.findById(id).orElseThrow(); }
    private Payment payment(Long id) { return payments.findByOrderId(id).get(0); }
    private int stock(Fixture f) { return stocks.findByBranchIdAndProductId(f.branch().getId(), f.product().getId()).orElseThrow().getQuantity(); }
    private long orderCount(User customer) { return orders.findByUserIdOrderByCreatedAtDesc(customer.getId(), Pageable.unpaged()).getTotalElements(); }
    private List<vn.iotstar.domain.entity.StockLog> movements(Fixture f) { return logs.findByBranchIdOrderByCreatedAtDesc(f.branch().getId(), Pageable.unpaged()).getContent(); }
    private record Fixture(User customer, User actor, Branch branch, Product product, Cart cart) { }
}
