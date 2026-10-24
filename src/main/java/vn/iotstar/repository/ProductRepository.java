package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.Product;
import vn.iotstar.domain.enums.*;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Query("select new vn.iotstar.dto.product.ProductOptionDto(p.id, p.name, p.sku) from Product p where p.status = vn.iotstar.domain.enums.ProductStatus.ACTIVE order by p.name")
    List<vn.iotstar.dto.product.ProductOptionDto> findActiveOptions();
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String name, Pageable pageable);

    Optional<Product> findBySku(String sku);
}
