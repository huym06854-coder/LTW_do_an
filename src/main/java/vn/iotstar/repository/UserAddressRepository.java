package vn.iotstar.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.domain.entity.UserAddress;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserId(Long userId);

    List<UserAddress> findByUserIdOrderByIsDefaultDescIdAsc(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);
}
