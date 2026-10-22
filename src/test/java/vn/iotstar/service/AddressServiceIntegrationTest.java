package vn.iotstar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.HtmlUtils;
import vn.iotstar.domain.entity.User;
import vn.iotstar.dto.address.AddressDto;
import vn.iotstar.dto.address.AddressRequest;
import vn.iotstar.dto.auth.RegisterDto;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.security.CustomerUserDetails;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:address-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.locations=classpath:db/migration/h2",
        "spring.jpa.hibernate.ddl-auto=validate",
        "ADMIN_BOOTSTRAP_USERNAME=", "ADMIN_BOOTSTRAP_EMAIL=", "ADMIN_BOOTSTRAP_PASSWORD=", "ADMIN_BOOTSTRAP_FULL_NAME=",
        "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD=", "ADMIN_FULL_NAME="
})
@AutoConfigureMockMvc
class AddressServiceIntegrationTest {
    @Autowired private AddressService addresses;
    @Autowired private UserService users;
    @Autowired private MockMvc mvc;

    @Test
    void usersCannotReadEditDeleteOrSelectSomeoneElsesAddress() {
        User owner = account();
        User other = account();
        AddressDto address = addresses.save(owner.getId(), null, request("Nhà riêng", true));

        assertThatThrownBy(() -> addresses.getOwned(other.getId(), address.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> addresses.save(other.getId(), address.id(), request("Bị sửa", true))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> addresses.delete(other.getId(), address.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> addresses.makeDefault(other.getId(), address.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(addresses.list(other.getId())).isEmpty();
        assertThat(addresses.getOwned(owner.getId(), address.id()).recipientName()).isEqualTo("Nhà riêng");
    }

    @Test
    void addressBookHasOneDefaultAndPromotesAnotherAfterDefaultDeletion() {
        User owner = account();
        AddressDto first = addresses.save(owner.getId(), null, request("Nhà riêng", false));
        AddressDto second = addresses.save(owner.getId(), null, request("Công ty", true));
        assertThat(first.isDefault()).isTrue();
        assertThat(addresses.getOwned(owner.getId(), first.id()).isDefault()).isFalse();
        assertThat(second.isDefault()).isTrue();

        addresses.makeDefault(owner.getId(), first.id());
        assertThat(addresses.list(owner.getId()).stream().filter(AddressDto::isDefault)).hasSize(1);
        addresses.delete(owner.getId(), first.id());
        assertThat(addresses.getOwned(owner.getId(), second.id()).isDefault()).isTrue();
        assertThat(second.fullAddress()).isEqualTo("25 Nguyễn Trãi, Phường 1, Quận 1, TP. Hồ Chí Minh");
    }

    @Test
    void concurrentAddressCreationSerializesTheDefaultSelection() throws Exception {
        User owner = account();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                return addresses.save(owner.getId(), null, request("Nhà riêng", true));
            });
            var second = workers.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                return addresses.save(owner.getId(), null, request("Công ty", true));
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        var saved = addresses.list(owner.getId());
        assertThat(saved).hasSize(2);
        assertThat(saved.stream().filter(AddressDto::isDefault)).hasSize(1);
    }

    @Test
    void addressFormShowsValidationErrorsAndPreservesTheEnteredFields() throws Exception {
        User owner = account();
        var principal = user(new CustomerUserDetails(owner));
        var result = mvc.perform(post("/profile/addresses").with(principal).with(csrf())
                        .param("recipientName", "Nguyễn Khách Hàng").param("phone", "invalid")
                        .param("province", "TP. Hồ Chí Minh").param("district", "Quận 1")
                        .param("ward", "Phường 1").param("addressDetail", "25 Nguyễn Trãi")
                        .param("_defaultAddress", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?section=addresses#address-form"))
                .andReturn();
        assertThat(result.getFlashMap().containsKey(BindingResult.MODEL_KEY_PREFIX + "addressForm")).isTrue();
        BindingResult validation = (BindingResult) result.getFlashMap().get(BindingResult.MODEL_KEY_PREFIX + "addressForm");
        assertThat(validation.hasFieldErrors("phone")).isTrue();
        assertThat(validation.hasFieldErrors("defaultAddress")).isFalse();
        mvc.perform(get("/profile").param("section", "addresses").with(principal).flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("addressForm", "phone"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Số điện thoại phải có")))
                .andExpect(response -> assertThat(HtmlUtils.htmlUnescape(response.getResponse().getContentAsString()))
                        .containsPattern("<input\\b(?=[^>]*\\bname=\"addressDetail\")(?=[^>]*\\bvalue=\"25 Nguyễn Trãi\")[^>]*>"));
        assertThat(addresses.list(owner.getId())).isEmpty();
    }

    @Test
    void addressManagementRendersSavedAddressesAndEditFormWithCsrfProtection() throws Exception {
        User owner = account();
        AddressDto address = addresses.save(owner.getId(), null, request("Nhà riêng", true));
        var principal = user(new CustomerUserDetails(owner));
        mvc.perform(get("/profile").param("section", "addresses")
                        .param("editAddressId", address.id().toString()).with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sổ địa chỉ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu thay đổi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(post("/profile/addresses/" + address.id() + "/delete").with(principal))
                .andExpect(status().isForbidden());
        assertThat(addresses.list(owner.getId())).hasSize(1);
    }

    private User account() {
        String suffix = UUID.randomUUID().toString();
        return users.register(new RegisterDto("Khách hàng", "addr-" + suffix,
                "addr-" + suffix + "@example.com", "password-123", "0901234567"));
    }

    private AddressRequest request(String name, boolean makeDefault) {
        return new AddressRequest(name, "0901234567", "TP. Hồ Chí Minh", "Quận 1", "Phường 1", "25 Nguyễn Trãi", makeDefault);
    }
}
