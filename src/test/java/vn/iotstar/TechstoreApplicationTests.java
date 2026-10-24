package vn.iotstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.domain.entity.Role;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.UserRole;
import vn.iotstar.dto.auth.RegisterDto;
import vn.iotstar.security.CustomerUserDetails;
import vn.iotstar.service.UserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:techstore-test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.flyway.locations=classpath:db/migration/h2",
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.mail.host=localhost",
		"spring.mail.port=2525",
		"spring.mail.username=",
		"spring.mail.password=",
		"ADMIN_BOOTSTRAP_USERNAME=",
		"ADMIN_BOOTSTRAP_EMAIL=",
		"ADMIN_BOOTSTRAP_PASSWORD=",
		"ADMIN_BOOTSTRAP_FULL_NAME=",
		"ADMIN_USERNAME=",
		"ADMIN_EMAIL=",
		"ADMIN_PASSWORD=",
		"ADMIN_FULL_NAME="
})
@AutoConfigureMockMvc
class TechstoreApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserService userService;

	@Test
	void contextLoads() {
	}

	@Test
	void storefrontPagesAreRoutedToThymeleafTemplates() throws Exception {
		String[][] pages = {
				{ "/", "store/index" },
				{ "/products", "store/products" },
				{ "/login", "store/login" },
				{ "/register", "auth/register" },
				{ "/about", "store/about" },
				{ "/contact", "store/contact" },
				{ "/news", "store/news" },
				{ "/offers", "store/offers" }
		};

		for (String[] page : pages) {
			mockMvc.perform(get(page[0]))
					.andExpect(status().isOk())
					.andExpect(view().name(page[1]))
					.andExpect(content().contentTypeCompatibleWith("text/html"));
		}
	}

	@Test
	void authenticatedHeaderShowsProfileAndLogoutInsteadOfLogin() throws Exception {
		String suffix = java.util.UUID.randomUUID().toString();
		User account = userService.register(new RegisterDto("Nguyễn Văn Thành viên", "member-" + suffix,
				"member-" + suffix + "@example.com", "secure-pass-123", null));

		mockMvc.perform(get("/login").with(user(new CustomerUserDetails(account))))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Văn Thành viên")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hồ sơ cá nhân")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng xuất")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("class=\"header-action login\""))));
	}

	@Test
	void profilePageShowsOrderHistoryAndPersonalInformationSections() throws Exception {
		String suffix = java.util.UUID.randomUUID().toString();
		User account = userService.register(new RegisterDto("Nguyễn Khách Hàng", "profile-" + suffix,
				"profile-" + suffix + "@example.com", "secure-pass-123", "0900000000"));
		CustomerUserDetails principal = new CustomerUserDetails(account);

		mockMvc.perform(get("/profile").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(view().name("account/profile"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đơn hàng đã mua")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("chưa có đơn hàng")));

		mockMvc.perform(get("/profile").param("section", "info").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Thông tin của tôi")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(account.getEmail())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Số điện thoại")));
	}

	@Test
	void registeringWithAnExistingEmailReturnsToTheFormWithHelpfulMessage() throws Exception {
		String email = "existing-" + java.util.UUID.randomUUID() + "@example.com";
		String password = "secure-pass-123";

		mockMvc.perform(post("/register").with(csrf())
				.param("fullName", "Khách hàng thử nghiệm")
				.param("username", "user-" + java.util.UUID.randomUUID())
				.param("email", email)
				.param("password", password))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(post("/register").with(csrf())
				.param("fullName", "Khách hàng thử nghiệm")
				.param("username", "another-" + java.util.UUID.randomUUID())
				.param("email", email)
				.param("password", password))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/register"))
				.andExpect(flash().attribute("registrationError",
						"Email hoặc tên đăng nhập đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng thông tin khác."));
	}

	@Test
	void guestIsRedirectedFromAuthenticatedStoreAndAdminPages() throws Exception {
		for (String path : new String[] { "/cart", "/checkout", "/orders", "/admin", "/staff/orders" }) {
			mockMvc.perform(get(path))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("/login"));
		}
	}

	@Test
	void customerCannotAccessAdminOrStaffPages() throws Exception {
		mockMvc.perform(get("/admin").with(user("customer").roles("CUSTOMER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/staff/orders").with(user("customer").roles("CUSTOMER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void productCatalogApiIsPublic() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"));
	}

	@Test
	void adminDashboardAndItsAssetsAreAvailable() throws Exception {
		mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/index"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Quản trị chuỗi cửa hàng")));

		mockMvc.perform(get("/admin/css/admin.css").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/admin/js/admin.js").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/javascript"));
	}

}
