package vn.iotstar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import vn.iotstar.repository.BranchRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:branch-unicode-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.locations=classpath:db/migration/h2",
        "spring.jpa.hibernate.ddl-auto=validate",
        "ADMIN_BOOTSTRAP_USERNAME=", "ADMIN_BOOTSTRAP_EMAIL=", "ADMIN_BOOTSTRAP_PASSWORD=",
        "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD="
})
@AutoConfigureMockMvc
class BranchUnicodeIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private BranchService service;
    @Autowired private BranchRepository repository;

    @Test
    void adminCanCorrectAndDisplayVietnameseBranchDetails() throws Exception {
        String code = "UNI-" + UUID.randomUUID().toString().substring(0, 8);
        long id = service.create(code, "Chi nh?nh", "15 V? Văn Ngân").getId();

        mvc.perform(post("/admin/branches/{id}", id)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .param("name", "Chi nhánh Thủ Đức")
                        .param("address", "15 Võ Văn Ngân, TP. Thủ Đức"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/branches"));

        var branch = repository.findById(id).orElseThrow();
        assertThat(branch.getName()).isEqualTo("Chi nhánh Thủ Đức");
        assertThat(branch.getAddress()).isEqualTo("15 Võ Văn Ngân, TP. Thủ Đức");

        mvc.perform(get("/admin/branches").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chi nhánh Thủ Đức")))
                .andExpect(content().string(containsString("15 Võ Văn Ngân, TP. Thủ Đức")));
    }
}
