package vn.iotstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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
		"spring.mail.password="
})
@AutoConfigureMockMvc
class TechstoreApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void storefrontPagesAreRoutedToThymeleafTemplates() throws Exception {
		String[][] pages = {
				{ "/", "store/index" },
				{ "/products", "store/products" },
				{ "/product-detail?id=1", "store/product-detail" },
				{ "/cart", "store/cart" },
				{ "/login", "store/login" },
				{ "/register", "auth/register" },
				{ "/checkout", "cart/checkout" },
				{ "/profile", "account/profile" },
				{ "/orders", "orders/list" },
				{ "/orders/TD-1042", "orders/detail" },
				{ "/staff/orders", "staff/orders" },
				{ "/staff/orders/TD-1042", "staff/order-detail" },
				{ "/about", "store/about" },
				{ "/contact", "store/contact" },
				{ "/news", "store/news" },
				{ "/offers", "store/offers" },
				{ "/admin", "admin/index" }
		};

		for (String[] page : pages) {
			mockMvc.perform(get(page[0]))
					.andExpect(status().isOk())
					.andExpect(view().name(page[1]))
					.andExpect(content().contentTypeCompatibleWith("text/html"));
		}
	}

	@Test
	void adminDashboardAndItsAssetsAreAvailable() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/index"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng quan hệ thống")));

		mockMvc.perform(get("/admin/css/admin.css"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/admin/js/admin.js"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/javascript"));
	}

}
