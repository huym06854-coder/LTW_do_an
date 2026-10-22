package vn.iotstar;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.Brand;
import vn.iotstar.domain.entity.Category;
import vn.iotstar.domain.entity.Product;
import vn.iotstar.domain.enums.ProductStatus;
import vn.iotstar.repository.BrandRepository;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.service.ProductService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:product-catalog-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.flyway.locations=classpath:db/migration/h2", "spring.jpa.hibernate.ddl-auto=validate",
    "ADMIN_BOOTSTRAP_USERNAME=", "ADMIN_BOOTSTRAP_EMAIL=", "ADMIN_BOOTSTRAP_PASSWORD=",
    "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD="
})
@AutoConfigureMockMvc
@Transactional
class ProductCatalogIntegrationTests {
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductService service;
    @Autowired MockMvc mvc;

    @Test
    void filteringSortingAndPaginationIncludeMatchesBeyondTheFirstHundredProducts() throws Exception {
        Category category = new Category();
        category.setName("RegressionCategory");
        categories.save(category);
        Brand brand = new Brand();
        brand.setName("RegressionBrand");
        brands.save(brand);

        for (int index = 0; index < 110; index++) {
            saveProduct("Catalog regression " + index, "REG-" + index, 1000 + index, category, brand, ProductStatus.ACTIVE);
        }
        saveProduct("Catalog regression inactive", "REG-INACTIVE", 1, category, brand, ProductStatus.INACTIVE);
        saveProduct("Catalog regression different brand", "REG-NOBRAND", 1, category, null, ProductStatus.ACTIVE);
        saveProduct("Unrelated name", "REG-UNRELATED", 1, category, brand, ProductStatus.ACTIVE);
        products.flush();

        var first = service.catalog("Catalog regression", "RegressionCategory", "RegressionBrand", "under10",
            PageRequest.of(0, 12, ProductService.catalogSort("price-asc")));
        assertThat(first.getTotalElements()).isEqualTo(110);
        assertThat(first.getTotalPages()).isEqualTo(10);
        assertThat(first.getContent()).hasSize(12);
        assertThat(first.getContent().getFirst().price()).isEqualByComparingTo("1000");

        var last = service.catalog("Catalog regression", "RegressionCategory", "RegressionBrand", "under10",
            PageRequest.of(9, 12, ProductService.catalogSort("price-asc")));
        assertThat(last.getContent()).hasSize(2);
        assertThat(last.getContent().getLast().name()).isEqualTo("Catalog regression 109");

        mvc.perform(get("/products").param("q", "Catalog regression").param("category", "RegressionCategory")
                .param("brand", "RegressionBrand").param("price", "under10").param("sort", "price-asc").param("page", "9"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Catalog regression 109")))
            .andExpect(content().string(containsString("110 sản phẩm")))
            .andExpect(result -> {
                var previousLink = java.util.regex.Pattern.compile("<a\\s+rel=\"prev\"\\s+href=\"([^\"]+)\"")
                    .matcher(result.getResponse().getContentAsString());
                assertThat(previousLink.find()).isTrue();
                assertThat(previousLink.group(1)).contains("category=RegressionCategory", "brand=RegressionBrand",
                    "price=under10", "sort=price-asc", "page=8");
            });

        mvc.perform(get("/api/products").param("q", "Catalog regression").param("category", "RegressionCategory")
                .param("brand", "RegressionBrand").param("price", "under10").param("sort", "price-desc").param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(110))
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.content[0].name").value("Catalog regression 109"));
    }

    private void saveProduct(String name, String sku, int price, Category category, Brand brand, ProductStatus status) {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setPrice(BigDecimal.valueOf(price));
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus(status);
        products.save(product);
    }
}
