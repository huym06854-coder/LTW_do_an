package vn.iotstar.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.iotstar.domain.entity.Branch;
import vn.iotstar.domain.entity.BranchStock;
import vn.iotstar.domain.entity.Brand;
import vn.iotstar.domain.entity.Cart;
import vn.iotstar.domain.entity.CartItem;
import vn.iotstar.domain.entity.Category;
import vn.iotstar.domain.entity.Coupon;
import vn.iotstar.domain.entity.Notification;
import vn.iotstar.domain.entity.Order;
import vn.iotstar.domain.entity.OrderItem;
import vn.iotstar.domain.entity.OrderStatusHistory;
import vn.iotstar.domain.entity.PasswordResetToken;
import vn.iotstar.domain.entity.Payment;
import vn.iotstar.domain.entity.Product;
import vn.iotstar.domain.entity.ProductImage;
import vn.iotstar.domain.entity.ProductSpecification;
import vn.iotstar.domain.entity.Review;
import vn.iotstar.domain.entity.Role;
import vn.iotstar.domain.entity.StockLog;
import vn.iotstar.domain.entity.Transfer;
import vn.iotstar.domain.entity.TransferItem;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.entity.UserAddress;

@DataJpaTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:entity-schema;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.flyway.locations=classpath:db/migration/h2",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class EntitySchemaTest {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void allEntitiesMapToTheMigratedSchema() {
		var metamodel = entityManagerFactory.getMetamodel();
        assertThat(metamodel.entity(vn.iotstar.domain.entity.TransferStatusHistory.class)).isNotNull();
        assertThat(metamodel.entity(vn.iotstar.domain.entity.EmployeeAssignmentHistory.class)).isNotNull();

		assertThat(metamodel.getEntities()).hasSize(25);
		assertThat(metamodel.entity(User.class)).isNotNull();
		assertThat(metamodel.entity(Role.class)).isNotNull();
		assertThat(metamodel.entity(Branch.class)).isNotNull();
		assertThat(metamodel.entity(BranchStock.class)).isNotNull();
		assertThat(metamodel.entity(Category.class)).isNotNull();
		assertThat(metamodel.entity(Brand.class)).isNotNull();
		assertThat(metamodel.entity(Product.class)).isNotNull();
		assertThat(metamodel.entity(ProductImage.class)).isNotNull();
		assertThat(metamodel.entity(ProductSpecification.class)).isNotNull();
		assertThat(metamodel.entity(Review.class)).isNotNull();
		assertThat(metamodel.entity(UserAddress.class)).isNotNull();
		assertThat(metamodel.entity(PasswordResetToken.class)).isNotNull();
		assertThat(metamodel.entity(StockLog.class)).isNotNull();
		assertThat(metamodel.entity(Transfer.class)).isNotNull();
		assertThat(metamodel.entity(TransferItem.class)).isNotNull();
		assertThat(metamodel.entity(Cart.class)).isNotNull();
		assertThat(metamodel.entity(CartItem.class)).isNotNull();
		assertThat(metamodel.entity(Order.class)).isNotNull();
		assertThat(metamodel.entity(OrderItem.class)).isNotNull();
		assertThat(metamodel.entity(OrderStatusHistory.class)).isNotNull();
		assertThat(metamodel.entity(Coupon.class)).isNotNull();
		assertThat(metamodel.entity(Payment.class)).isNotNull();
		assertThat(metamodel.entity(Notification.class)).isNotNull();
		assertThat(jdbcTemplate.queryForList("SELECT name FROM roles ORDER BY id", String.class))
				.containsExactly("ADMIN", "MANAGER", "STAFF", "CUSTOMER");
	}
}
