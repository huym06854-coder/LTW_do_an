package vn.iotstar;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.iotstar.domain.entity.*;
import vn.iotstar.domain.enums.*;
import vn.iotstar.repository.*;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:operations-workspace;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.flyway.locations=classpath:db/migration/h2",
    "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
    "ADMIN_BOOTSTRAP_USERNAME=", "ADMIN_BOOTSTRAP_EMAIL=", "ADMIN_BOOTSTRAP_PASSWORD=",
    "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD="
})
@AutoConfigureMockMvc
class OperationsWorkspaceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired BranchRepository branches;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired BranchStockRepository stocks;
    @Autowired TransferRepository transferRepository;
    @Autowired UserService userService;
    @Autowired TransferService transfers;
    @Autowired InventoryService inventory;
    @Autowired AdminUserService adminUsers;
    @Autowired PasswordEncoder encoder;
    @Autowired BranchService branchService;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired TransferStatusHistoryRepository transferHistories;

    private Branch source, destination;
    private User admin, manager, staff, receiver, customer;
    private Product product;
    private String key;

    @BeforeEach
    void fixture() {
        key = UUID.randomUUID().toString().substring(0, 8);
        source = branch("TD-" + key, "TechStore Thủ Đức");
        destination = branch("Q1-" + key, "TechStore Quận 1");
        admin = account(UserRole.ADMIN, null, "Nguyễn Quản Trị");
        manager = account(UserRole.MANAGER, source, "Trần Quản Lý");
        staff = account(UserRole.STAFF, source, "Nguyễn Minh Anh");
        receiver = account(UserRole.STAFF, destination, "Lê Nhận Hàng");
        customer = account(UserRole.CUSTOMER, null, "Khách Hàng");
        Category category = new Category();
        category.setName("Điện thoại " + key);
        category = categories.save(category);
        product = new Product();
        product.setSku("IPHONE-" + key);
        product.setName("iPhone 16 Pro Max 256GB");
        product.setPrice(new BigDecimal("29990000"));
        product.setCategory(category);
        product = products.save(product);
        stock(source, product, 12);
        stock(destination, product, 3);
    }

    @Test
    void operationsPagesRenderRealRelationsWithOpenInViewDisabled() throws Exception {
        transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 2), staff);
        for (String path : List.of("/admin", "/admin/branches", "/admin/users", "/admin/stock", "/admin/transfers")) {
            var result = mvc.perform(get(path).with(user(principal(admin)))).andExpect(status().isOk()).andReturn();
            snapshot(path, result);
        }
        for (String path : List.of("/staff", "/staff/stock", "/staff/transfers", "/staff/orders")) {
            var result = mvc.perform(get(path).with(user(principal(staff)))).andExpect(status().isOk())
                    .andExpect(content().string(containsString("TechStore Thủ Đức"))).andReturn();
            snapshot(path, result);
        }
        mvc.perform(get("/staff/stock").with(user(principal(staff))))
                .andExpect(content().string(containsString(product.getName())));
        var loaded = userService.byId(staff.getId());
        assertThat(loaded.getRole().getName()).isEqualTo(UserRole.STAFF);
        assertThat(loaded.getBranch().getName()).isEqualTo(source.getName());
    }

    @Test
    void transferButtonsFollowRoleAndSourceOrDestinationBranch() throws Exception {
        var pending = transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 2), staff);
        String approve = "/staff/transfers/" + pending.getId() + "/approve";
        mvc.perform(get("/staff/transfers").with(user(principal(staff))))
                .andExpect(content().string(not(containsString("action=\"" + approve + "\""))));
        mvc.perform(get("/staff/transfers").with(user(principal(manager))))
                .andExpect(content().string(containsString("action=\"" + approve + "\"")));
        transfers.approve(pending.getId(), manager);
        String ship = "/staff/transfers/" + pending.getId() + "/ship";
        mvc.perform(get("/staff/transfers").with(user(principal(receiver))))
                .andExpect(content().string(not(containsString("action=\"" + ship + "\""))));
        mvc.perform(get("/staff/transfers").with(user(principal(staff))))
                .andExpect(content().string(containsString("action=\"" + ship + "\"")));
        transfers.ship(pending.getId(), staff);
        String complete = "/staff/transfers/" + pending.getId() + "/complete";
        mvc.perform(get("/staff/transfers").with(user(principal(staff))))
                .andExpect(content().string(not(containsString("action=\"" + complete + "\""))));
        mvc.perform(get("/staff/transfers").with(user(principal(receiver))))
                .andExpect(content().string(containsString("action=\"" + complete + "\"")));
    }

    @Test
    void realTransferEndpointsRejectWrongBranchAndApplyEachStockChangeOnce() throws Exception {
        var transfer = transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 2), staff);
        String base = "/staff/transfers/" + transfer.getId();
        mvc.perform(post(base + "/approve").with(user(principal(staff))).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("error"));
        User otherManager = account(UserRole.MANAGER, destination, "Quản Lý Khác");
        mvc.perform(post(base + "/approve").with(user(principal(otherManager))).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(base + "/approve").with(user(principal(manager))).with(csrf()))
                .andExpect(redirectedUrl("/staff/transfers"));
        mvc.perform(post(base + "/approve").with(user(principal(manager))).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(10);
        mvc.perform(post(base + "/ship").with(user(principal(receiver))).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(base + "/ship").with(user(principal(staff))).with(csrf()))
                .andExpect(redirectedUrl("/staff/transfers"));
        mvc.perform(post(base + "/complete").with(user(principal(staff))).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(base + "/complete").with(user(principal(receiver))).with(csrf()))
                .andExpect(redirectedUrl("/staff/transfers"));
        mvc.perform(post(base + "/complete").with(user(principal(receiver))).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertThat(inventory.available(destination.getId(), product.getId())).isEqualTo(5);
    }

    @Test
    void cancellingApprovedTransferTwiceRestoresOnlyOnce() {
        var transfer = transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 2), staff);
        transfers.approve(transfer.getId(), manager);
        transfers.cancel(transfer.getId(), staff);
        assertThatThrownBy(() -> transfers.cancel(transfer.getId(), staff))
                .isInstanceOf(vn.iotstar.exception.BusinessException.class);
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(12);
    }

    @Test
    void staffStockUpdateUsesAssignedBranchAndRejectsInvalidAdjustments() throws Exception {
        mvc.perform(post("/staff/stock/update").with(user(principal(staff))).with(csrf())
                .param("branchId", destination.getId().toString()).param("productId", product.getId().toString())
                .param("quantityChange", "-1").param("reason", "Kiểm kê thực tế"))
                .andExpect(redirectedUrl("/staff/stock")).andExpect(flash().attributeExists("success"));
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(11);
        assertThat(inventory.available(destination.getId(), product.getId())).isEqualTo(3);
        mvc.perform(post("/staff/stock/update").with(user(principal(staff))).with(csrf())
                .param("productId", product.getId().toString()).param("quantityChange", "-99").param("reason", "Kiểm kê"))
                .andExpect(flash().attributeExists("error"));
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(11);
        assertThatThrownBy(() -> inventory.adminAdjust(destination.getId(), product.getId(), 5, "Sai chi nhánh", userService.byId(staff.getId())))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void adminCanCreateEmployeeWithHashedPasswordAndAssignAnotherBranch() throws Exception {
        String login = "employee-" + key;
        mvc.perform(post("/admin/users/employees").with(user(principal(admin))).with(csrf())
                .param("fullName", "Trần Nhân Viên").param("username", login).param("email", login + "@example.com")
                .param("password", "EmployeeTest123!").param("role", "STAFF").param("branchId", source.getId().toString()))
                .andExpect(redirectedUrl("/admin/users")).andExpect(flash().attributeExists("success"));
        var created = users.findByUsernameIgnoreCase(login).orElseThrow();
        assertThat(encoder.matches("EmployeeTest123!", created.getPassword())).isTrue();
        assertThat(created.getPassword()).isNotEqualTo("EmployeeTest123!");
        var loaded = userService.byId(created.getId());
        assertThat(loaded.getRole().getName()).isEqualTo(UserRole.STAFF);
        assertThat(loaded.getBranch().getId()).isEqualTo(source.getId());
        mvc.perform(post("/admin/users/{id}/branch", created.getId()).with(user(principal(admin))).with(csrf())
                .param("branchId", destination.getId().toString())).andExpect(redirectedUrl("/admin/users"));
        assertThat(userService.byId(created.getId()).getBranch().getId()).isEqualTo(destination.getId());
    }

    @Test
    void employeeCreationCannotGrantAdminAndDoesNotEchoPasswordsOnValidationError() throws Exception {
        String login = "forbidden-" + key;
        mvc.perform(post("/admin/users/employees").with(user(principal(admin))).with(csrf())
                .param("fullName", "Nhân viên").param("username", login).param("email", login + "@example.com")
                .param("password", "NeverEchoThis123!").param("role", "ADMIN").param("branchId", source.getId().toString()))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("employeeForm"))
                .andExpect(content().string(not(containsString("NeverEchoThis123!"))));
        assertThat(users.existsByUsernameIgnoreCase(login)).isFalse();
        mvc.perform(post("/admin/users/{id}/branch", customer.getId()).with(user(principal(admin))).with(csrf())
                .param("branchId", source.getId().toString())).andExpect(flash().attributeExists("error"));
        assertThat(userService.byId(customer.getId()).getBranch()).isNull();
    }

    @Test
    void customerCannotReachOperatorEndpointsAndStaffCanLoadSharedAssets() throws Exception {
        for (String path : List.of("/admin/users", "/admin/stock", "/staff/stock", "/staff/transfers")) {
            mvc.perform(get(path).with(user(principal(customer)))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/admin/users/{id}/branch", staff.getId()).with(user(principal(staff))).with(csrf())
                .param("branchId", destination.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/css/admin.css").with(user(principal(staff)))).andExpect(status().isOk());
        mvc.perform(get("/admin/js/admin-shell.js").with(user(principal(staff)))).andExpect(status().isOk());
        mvc.perform(post("/staff/stock/update").with(user(principal(staff)))
                .param("productId", product.getId().toString()).param("quantityChange", "2").param("reason", "No CSRF"))
                .andExpect(status().isForbidden());
    }

    @Test
    void branchReassignmentUsesCurrentDatabaseScopeWithoutRelogin() throws Exception {
        CustomerUserDetails existingSession = principal(staff);
        adminUsers.assignBranch(staff.getId(), destination.getId(), admin.getId());
        mvc.perform(get("/staff/stock").with(user(existingSession))).andExpect(status().isOk())
                .andExpect(content().string(containsString("TechStore Quận 1")))
                .andExpect(model().attribute("lowStockCount", 1L));
        User unassigned = account(UserRole.STAFF, null, "Chưa Phân Công");
        mvc.perform(get("/staff").with(user(principal(unassigned)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn chưa được phân công chi nhánh")));
    }

    private Branch branch(String code, String name) {
        var b = new Branch(); b.setCode(code); b.setName(name); b.setAddress("15 Võ Văn Ngân, TP. Hồ Chí Minh");
        return branches.save(b);
    }

    @Test
    void lockingARealLoginInvalidatesTheOldWebSessionForStaffAndAdmin() throws Exception {
        User secondAdmin = account(UserRole.ADMIN, null, "Admin kiểm thử thu hồi phiên");
        for (User target : List.of(staff, secondAdmin)) {
            for (UserStatus state : List.of(UserStatus.LOCKED, UserStatus.INACTIVE)) {
                adminUsers.changeStatus(target.getId(), admin.getId(), UserStatus.ACTIVE);
                var session = login(target);
                adminUsers.changeStatus(target.getId(), admin.getId(), state);
                String route = target.getRole().getName() == UserRole.ADMIN ? "/admin/users" : "/profile";
                mvc.perform(get(route).session(session)).andExpect(redirectedUrl("/login?accountDisabled"));
                assertThat(session.isInvalid()).isTrue();
            }
        }
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(12);
    }

    @Test
    void rolePromotionAndDemotionRefreshAnExistingSessionAndKeepAssignmentHistory() throws Exception {
        var session = login(staff);
        var first = transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 2), staff);
        mvc.perform(post("/admin/users/{id}/assignment", staff.getId()).with(user(principal(admin))).with(csrf())
                .param("role", "MANAGER").param("branchId", source.getId().toString()))
                .andExpect(redirectedUrl("/admin/users/" + staff.getId()));
        mvc.perform(post("/staff/transfers/{id}/approve", first.getId()).session(session).with(csrf()))
                .andExpect(flash().attributeExists("success"));
        adminUsers.changeAssignment(staff.getId(), destination.getId(), UserRole.STAFF, admin.getId());
        var events = adminUsers.history(staff.getId());
        assertThat(events).hasSize(2);
        assertThat(events.getFirst().getPreviousBranch().getId()).isEqualTo(source.getId());
        assertThat(events.getFirst().getNewBranch().getId()).isEqualTo(destination.getId());
        assertThat(events.getFirst().getChangedBy().getId()).isEqualTo(admin.getId());
        var second = transfers.create(destination.getId(), source.getId(), Map.of(product.getId(), 1), receiver);
        mvc.perform(post("/staff/transfers/{id}/approve", second.getId()).session(session).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertThat(inventory.available(destination.getId(), product.getId())).isEqualTo(3);
        mvc.perform(get("/admin/users/{id}", staff.getId()).with(user(principal(admin))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Lịch sử phân công")));
        assertThatThrownBy(() -> adminUsers.changeAssignment(staff.getId(), destination.getId(), UserRole.ADMIN, admin.getId()))
                .isInstanceOf(vn.iotstar.exception.BusinessException.class);
    }

    @Test
    void reassignmentRevokesOldOrderScopeOnAnExistingLogin() throws Exception {
        var oldOrder = order(source);
        var newOrder = order(destination);
        var session = login(staff);
        mvc.perform(get("/staff/orders/{id}", oldOrder.getId()).session(session)).andExpect(status().isOk());
        adminUsers.assignBranch(staff.getId(), destination.getId(), admin.getId());
        mvc.perform(get("/staff/orders/{id}", oldOrder.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/staff/orders/{id}", newOrder.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(post("/admin/users/{id}/assignment", staff.getId()).session(session).with(csrf())
                .param("role", "MANAGER").param("branchId", source.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveBranchBlocksNewOrdersStockTransfersAndAssignments() throws Exception {
        branchService.changeStatus(source.getId(), BranchStatus.INACTIVE);
        mvc.perform(get("/staff").with(user(principal(staff)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Chi nhánh đang ngừng hoạt động")));
        for (String route : List.of("/staff/orders", "/staff/stock", "/staff/transfers"))
            mvc.perform(get(route).with(user(principal(staff)))).andExpect(status().isForbidden());
        assertThatThrownBy(() -> inventory.adminAdjust(source.getId(), product.getId(), 1, "Nhập kho", admin))
                .isInstanceOf(vn.iotstar.exception.BusinessException.class);
        assertThatThrownBy(() -> transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 1), admin))
                .isInstanceOf(vn.iotstar.exception.BusinessException.class);
        assertThatThrownBy(() -> adminUsers.changeAssignment(receiver.getId(), source.getId(), UserRole.STAFF, admin.getId()))
                .isInstanceOf(vn.iotstar.exception.BusinessException.class);
        long before = orderRepository.count();
        var checkout = new vn.iotstar.dto.order.CheckoutDto(source.getId(), "Khách kiểm thử", "0901234567", null,
                DeliveryMethod.PICKUP_AT_BRANCH, PaymentMethod.CASH_ON_DELIVERY, null, null, null, false,
                null, null, null, null, UUID.randomUUID().toString(), product.getId(), 1);
        assertThatThrownBy(() -> orderService.checkout(customer.getId(), checkout))
                .isInstanceOf(vn.iotstar.exception.BusinessException.class);
        assertThat(orderRepository.count()).isEqualTo(before);
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(12);
        mvc.perform(post("/admin/branches/{id}/status", source.getId()).with(user(principal(admin))).with(csrf())
                .param("status", "ACTIVE")).andExpect(flash().attributeExists("success"));
        mvc.perform(get("/staff/stock").with(user(principal(staff)))).andExpect(status().isOk());
    }

    @Test
    void branchCannotCloseWhileOrdersOrTransferWorkIsOpen() throws Exception {
        var pending = transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 1), staff);
        mvc.perform(post("/admin/branches/{id}/status", destination.getId()).with(user(principal(admin))).with(csrf())
                .param("status", "INACTIVE")).andExpect(flash().attributeExists("error"));
        assertThat(branchService.get(destination.getId()).getStatus()).isEqualTo(BranchStatus.ACTIVE);
        transfers.cancel(pending.getId(), staff);
        var openOrder = order(source);
        assertThatThrownBy(() -> branchService.changeStatus(source.getId(), BranchStatus.INACTIVE))
                .isInstanceOf(vn.iotstar.exception.BusinessException.class);
        openOrder.setStatus(OrderStatus.CANCELLED); orderRepository.save(openOrder);
        branchService.changeStatus(source.getId(), BranchStatus.INACTIVE);
        assertThat(branchService.get(source.getId()).getStatus()).isEqualTo(BranchStatus.INACTIVE);
    }

    @Test
    void branchDetailTabsRenderOnlyTheSelectedBranchAndShowRealMetrics() throws Exception {
        var ownOrder = order(source);
        var otherOrder = order(destination);
        var transfer = transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 2), staff);
        var summary = branchService.summaries().stream().filter(b -> b.id().equals(source.getId())).findFirst().orElseThrow();
        assertThat(summary.managerCount()).isEqualTo(1);
        assertThat(summary.staffCount()).isEqualTo(1);
        assertThat(summary.pendingOrders()).isEqualTo(1);
        assertThat(summary.pendingTransfers()).isEqualTo(1);
        for (String tab : List.of("overview", "employees", "orders", "stock", "transfers"))
            mvc.perform(get("/admin/branches/{id}", source.getId()).param("tab", tab).with(user(principal(admin))))
                    .andExpect(status().isOk()).andExpect(content().string(containsString(source.getName())));
        mvc.perform(get("/admin/branches/{id}", source.getId()).param("tab", "employees").with(user(principal(admin))))
                .andExpect(content().string(containsString(staff.getFullName())))
                .andExpect(content().string(not(containsString(receiver.getFullName()))));
        mvc.perform(get("/admin/branches/{id}", source.getId()).param("tab", "orders").with(user(principal(admin))))
                .andExpect(content().string(containsString(ownOrder.getOrderNumber())))
                .andExpect(content().string(not(containsString(otherOrder.getOrderNumber()))));
        for (String route : List.of("/admin/transfers/", "/staff/transfers/"))
            mvc.perform(get(route + transfer.getId()).with(user(principal(route.startsWith("/admin") ? admin : staff))))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Lịch sử xử lý")))
                    .andExpect(content().string(containsString("Tạo yêu cầu chuyển kho")));
        var unrelated = account(UserRole.STAFF, branch("OTHER-" + key, "Chi nhánh khác"), "Nhân viên khác");
        mvc.perform(get("/staff/transfers/{id}", transfer.getId()).with(user(principal(unrelated))))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeFiltersSeparateCustomersAndKeepRoleBranchStatusAndKeyword() throws Exception {
        var result = mvc.perform(get("/admin/users").with(user(principal(admin)))
                .param("group", "employees").param("role", "MANAGER").param("status", "ACTIVE")
                .param("branchId", source.getId().toString()).param("q", manager.getEmail()))
                .andExpect(status().isOk()).andReturn();
        var page = (org.springframework.data.domain.Page<?>) result.getModelAndView().getModel().get("userPage");
        assertThat(page.getContent()).hasSize(1);
        assertThat(((User) page.getContent().getFirst()).getId()).isEqualTo(manager.getId());
        result = mvc.perform(get("/admin/users").with(user(principal(admin)))
                .param("group", "customers").param("q", customer.getEmail())).andExpect(status().isOk()).andReturn();
        page = (org.springframework.data.domain.Page<?>) result.getModelAndView().getModel().get("userPage");
        assertThat(page.getContent()).hasSize(1);
        assertThat(((User) page.getContent().getFirst()).getRole().getName()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void concurrentApprovalAndReceiptEachChangeStockAndHistoryOnlyOnce() throws Exception {
        var transfer = transfers.create(source.getId(), destination.getId(), Map.of(product.getId(), 2), staff);
        assertThat(runTwice(() -> transfers.approve(transfer.getId(), manager))).containsExactlyInAnyOrder(true, false);
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(10);
        transfers.ship(transfer.getId(), staff);
        assertThat(runTwice(() -> transfers.complete(transfer.getId(), receiver))).containsExactlyInAnyOrder(true, false);
        assertThat(inventory.available(destination.getId(), product.getId())).isEqualTo(5);
        var history = transferHistories.findByTransferIdOrderByCreatedAtAscIdAsc(transfer.getId());
        assertThat(history).hasSize(4);
        assertThat(history.stream().map(TransferStatusHistory::getNewStatus).toList())
                .containsExactly(TransferStatus.PENDING, TransferStatus.APPROVED, TransferStatus.IN_TRANSIT, TransferStatus.COMPLETED);
    }

    private org.springframework.mock.web.MockHttpSession login(User account) throws Exception {
        var persisted = userService.byId(account.getId());
        persisted.setPassword(encoder.encode("SessionTest123!")); users.save(persisted);
        var result = mvc.perform(post("/login").with(csrf()).param("login", account.getUsername())
                .param("password", "SessionTest123!")).andExpect(status().is3xxRedirection()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
        return session;
    }

    private vn.iotstar.domain.entity.Order order(Branch branch) {
        var order = new vn.iotstar.domain.entity.Order();
        order.setOrderNumber("WORK-" + UUID.randomUUID().toString().substring(0, 16));
        order.setUser(customer); order.setBranch(branch); order.setDeliveryMethod(DeliveryMethod.PICKUP_AT_BRANCH);
        order.setRecipientName("Khách kiểm thử"); order.setRecipientPhone("0901234567");
        order.setSubtotal(new BigDecimal("100000")); order.setShippingFee(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO); order.setTotalAmount(new BigDecimal("100000"));
        return orderRepository.save(order);
    }

    private List<Boolean> runTwice(Runnable operation) throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<Boolean> job = () -> {
            ready.countDown();
            if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
            try { operation.run(); return true; }
            catch (vn.iotstar.exception.BusinessException ex) { return false; }
        };
        try {
            var first = pool.submit(job); var second = pool.submit(job);
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS), second.get(15, java.util.concurrent.TimeUnit.SECONDS));
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test
    void userPaginationReturnsSeparatePagesAndStockKeepsSelectedBranch() throws Exception {
        for (int i = 0; i < 17; i++) account(UserRole.CUSTOMER, null, "Khách phân trang " + i);
        var first = mvc.perform(get("/admin/users").param("group", "all").with(user(principal(admin))))
                .andExpect(status().isOk()).andReturn();
        var second = mvc.perform(get("/admin/users").param("group", "all").param("page", "1").with(user(principal(admin))))
                .andExpect(status().isOk()).andReturn();
        var firstPage = (org.springframework.data.domain.Page<?>) first.getModelAndView().getModel().get("userPage");
        var secondPage = (org.springframework.data.domain.Page<?>) second.getModelAndView().getModel().get("userPage");
        assertThat(firstPage.getContent()).hasSize(15);
        var firstIds = firstPage.getContent().stream().map(u -> ((User) u).getId()).toList();
        var secondIds = secondPage.getContent().stream().map(u -> ((User) u).getId()).toList();
        assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
        assertThat(secondIds).isNotEmpty();
        mvc.perform(get("/admin/stock").param("branchId", destination.getId().toString()).with(user(principal(admin))))
                .andExpect(status().isOk()).andExpect(model().attribute("selectedBranchId", destination.getId()));
    }

    @Test
    void lockedStaffSessionCannotKeepViewingOrChangingBranchData() throws Exception {
        var existingSession = principal(staff);
        adminUsers.changeStatus(staff.getId(), admin.getId(), UserStatus.LOCKED);
        for (String route : List.of("/staff", "/staff/orders", "/staff/stock", "/staff/transfers")) {
            mvc.perform(get(route).with(user(existingSession))).andExpect(redirectedUrl("/login?accountDisabled"));
        }
        mvc.perform(post("/staff/stock/update").with(user(existingSession)).with(csrf())
                .param("productId", product.getId().toString()).param("quantityChange", "10").param("reason", "Thử tài khoản khóa"))
                .andExpect(redirectedUrl("/login?accountDisabled"));
        assertThat(inventory.available(source.getId(), product.getId())).isEqualTo(12);
    }

    private User account(UserRole role, Branch branch, String name) {
        var u = new User(); u.setFullName(name);
        String login = role.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
        u.setUsername(login); u.setEmail(login + "@example.com"); u.setPassword("{test-fixture}");
        u.setRole(roles.findByName(role).orElseThrow()); u.setBranch(branch);
        return users.save(u);
    }

    private void stock(Branch branch, Product p, int quantity) {
        var s = new BranchStock(); s.setBranch(branch); s.setProduct(p); s.setQuantity(quantity); stocks.save(s);
    }

    private CustomerUserDetails principal(User u) { return new CustomerUserDetails(u); }

    // Optional rendered output for manual browser QA; no live or production data is used.
    private void snapshot(String route, MvcResult result) throws Exception {
        String directory = System.getProperty("techstore.ui.snapshot-dir");
        if (directory == null) return;
        Path root = Path.of(directory);
        Files.createDirectories(root);
        Files.writeString(root.resolve(route.substring(1).replace('/', '-') + ".html"), result.getResponse().getContentAsString());
    }
}

