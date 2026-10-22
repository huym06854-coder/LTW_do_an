package vn.iotstar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.domain.entity.Role;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.*;
import vn.iotstar.dto.order.*;
import vn.iotstar.exception.BusinessException;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.OrderService;
import vn.iotstar.service.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-pages-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.locations=classpath:db/migration/h2",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.mail.host=localhost", "spring.mail.port=2525",
        "spring.mail.username=", "spring.mail.password=",
        "ADMIN_BOOTSTRAP_USERNAME=", "ADMIN_BOOTSTRAP_EMAIL=", "ADMIN_BOOTSTRAP_PASSWORD=", "ADMIN_BOOTSTRAP_FULL_NAME=",
        "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD=", "ADMIN_FULL_NAME="
})
@AutoConfigureMockMvc
class OrderPagesMvcIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService orders;
    @MockitoBean UserService users;

    private User account;

    @BeforeEach
    void setUp() {
        account = account(UserRole.STAFF);
        when(users.profileById(1L)).thenReturn(account);
    }

    @Test
    void allOrderListsRenderFiltersLocalizedStatusAndRealPagination() throws Exception {
        OrderDto row = new OrderDto(42L, "TD-PAGE-42", "READY_FOR_PICKUP", new BigDecimal("12030000"), 2L,
                List.of(new OrderItemDto(20L, "Laptop kiểm thử", 1, new BigDecimal("12000000"), new BigDecimal("12000000"))));
        var page = new PageImpl<>(List.of(row), PageRequest.of(0, 10), 15);
        when(orders.myOrders(eq(1L), nullable(OrderStatus.class), nullable(LocalDate.class), nullable(LocalDate.class), any(Pageable.class))).thenReturn(page);
        when(orders.managedOrders(any(User.class), nullable(OrderStatus.class), nullable(LocalDate.class), nullable(LocalDate.class), any(Pageable.class))).thenReturn(page);

        String[] paths = { "/orders", "/staff/orders", "/admin/orders" };
        UserRole[] roles = { UserRole.CUSTOMER, UserRole.STAFF, UserRole.ADMIN };
        for (int i = 0; i < paths.length; i++) {
            mvc.perform(get(paths[i]).param("status", "READY_FOR_PICKUP").param("from", "2026-10-01")
                            .param("to", "2026-10-22").with(user(principal(roles[i]))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("TD-PAGE-42")))
                    .andExpect(content().string(containsString("Sẵn sàng nhận hàng")))
                    .andExpect(content().string(containsString("Trang 1 / 2")))
                    .andExpect(content().string(containsString("page=1")))
                    .andExpect(content().string(containsString(paths[i] + "/42")));
        }
    }

    @Test
    void customerDetailShowsSnapshotTotalsAndTimelineWithoutOperatorControls() throws Exception {
        when(orders.myOrderDetail(1L, 42L)).thenReturn(detail(OrderStatus.CONFIRMED, List.of(), true, false));

        mvc.perform(get("/orders/42").with(user(principal(UserRole.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nguyễn Khách Hàng")))
                .andExpect(content().string(containsString("0901234567")))
                .andExpect(content().string(containsString("25 Nguyễn Trãi, TP. Hồ Chí Minh")))
                .andExpect(content().string(containsString("Chi nhánh Trung Tâm")))
                .andExpect(content().string(containsString("12.030.000 ₫")))
                .andExpect(content().string(containsString("Lịch sử xử lý")))
                .andExpect(content().string(containsString("22/10/2026 09:00")))
                .andExpect(content().string(containsString("/orders/42/cancel")))
                .andExpect(content().string(not(containsString("/orders/42/status"))))
                .andExpect(content().string(not(containsString("name=\"codCollected\""))));
    }

    @Test
    void staffAndAdminDetailsRenderOnlyAllowedActionsAndRequireCodConfirmation() throws Exception {
        when(orders.branchOrderDetail(eq(42L), any(User.class)))
                .thenReturn(detail(OrderStatus.SHIPPING, List.of(OrderStatus.COMPLETED, OrderStatus.DELIVERY_FAILED), false, false));

        String[] paths = { "/staff/orders/42", "/admin/orders/42" };
        UserRole[] roles = { UserRole.STAFF, UserRole.ADMIN };
        for (int i = 0; i < paths.length; i++) {
            mvc.perform(get(paths[i]).with(user(principal(roles[i]))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(paths[i] + "/status")))
                    .andExpect(content().string(containsString("name=\"codCollected\"")))
                    .andExpect(content().string(containsString("Tôi xác nhận đã thu đủ 12.030.000 ₫")))
                    .andExpect(content().string(containsString("value=\"COMPLETED\"")))
                    .andExpect(content().string(containsString("value=\"DELIVERY_FAILED\"")))
                    .andExpect(content().string(not(containsString("value=\"CONFIRMED\""))))
                    .andExpect(content().string(containsString("id=\"orderLiveNotice\"")))
                    .andExpect(content().string(containsString("/store/js/order-notifications.js")));
        }
    }

    @Test
    void statusActionPassesExplicitCodConfirmationAndKeepsBusinessErrorsOnDetailPage() throws Exception {
        mvc.perform(post("/staff/orders/42/status").with(user(principal(UserRole.STAFF))).with(csrf())
                        .param("status", "COMPLETED").param("codCollected", "true").param("note", "Đã thu tiền tại quầy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/orders/42"))
                .andExpect(flash().attributeExists("orderSuccess"));
        verify(orders).advanceStatus(42L, OrderStatus.COMPLETED, account, true, "Đã thu tiền tại quầy");

        doThrow(new BusinessException("Vui lòng xác nhận đã thu đủ tiền COD"))
                .when(orders).advanceStatus(42L, OrderStatus.COMPLETED, account, false, null);
        mvc.perform(post("/staff/orders/42/status").with(user(principal(UserRole.STAFF))).with(csrf())
                        .param("status", "COMPLETED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/orders/42"))
                .andExpect(flash().attribute("orderError", "Vui lòng xác nhận đã thu đủ tiền COD"));
    }

    @Test
    void refundNeedsExplicitConfirmationAndRendersOnlyWhenEligible() throws Exception {
        when(orders.branchOrderDetail(eq(42L), any(User.class)))
                .thenReturn(detail(OrderStatus.RETURNED, List.of(), false, true));
        mvc.perform(get("/admin/orders/42").with(user(principal(UserRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/orders/42/refund")))
                .andExpect(content().string(containsString("name=\"confirmedRefund\"")));
        mvc.perform(post("/admin/orders/42/refund").with(user(principal(UserRole.ADMIN))).with(csrf())
                        .param("note", "Chưa thực hiện hoàn tiền"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("orderError"));
        verify(orders, never()).confirmRefund(anyLong(), any(User.class), anyString());
    }

    private CustomerUserDetails principal(UserRole role) { return new CustomerUserDetails(account(role)); }

    private User account(UserRole role) {
        Role permission = new Role(); permission.setName(role);
        User user = new User(); user.setId(1L); user.setUsername("order-page-user");
        user.setFullName("Người dùng kiểm thử"); user.setPassword("encoded-password"); user.setRole(permission);
        return user;
    }

    private OrderDetailDto detail(OrderStatus status, List<OrderStatus> actions, boolean canCancel, boolean canRefund) {
        return new OrderDetailDto(42L, "TD-PAGE-42", status, "22/10/2026 09:00", null,
                2L, "Chi nhánh Trung Tâm", "120 Lê Lợi, TP. Hồ Chí Minh", DeliveryMethod.SHIP_TO_ADDRESS,
                "Nguyễn Khách Hàng", "0901234567", "25 Nguyễn Trãi, TP. Hồ Chí Minh", "Gọi trước khi giao",
                new BigDecimal("12000000"), new BigDecimal("30000"), BigDecimal.ZERO, new BigDecimal("12030000"),
                List.of(new OrderDetailItemDto(20L, "Laptop kiểm thử", "TEST-20", null, 1,
                        new BigDecimal("12000000"), new BigDecimal("12000000"))),
                PaymentMethod.CASH_ON_DELIVERY, canRefund ? PaymentStatus.PAID : PaymentStatus.PENDING,
                List.of(new OrderHistoryDto(null, OrderStatus.PENDING, "22/10/2026 09:00", "Nguyễn Khách Hàng", "Khách đặt hàng")),
                actions, canCancel, !canRefund, canRefund);
    }
}
