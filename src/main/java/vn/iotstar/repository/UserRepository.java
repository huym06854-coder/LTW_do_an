package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.*;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u from User u join fetch u.role r where u.status = :active and (r.name = :admin or (u.branch.id = :branchId and r.name in :roles))")
    List<User> findOrderOperators(@Param("branchId") Long branchId, @Param("active") UserStatus active,
            @Param("admin") UserRole admin, @Param("roles") Collection<UserRole> roles);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockForAddressBook(@Param("id") Long id);

    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleById(Long id);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select u from User u where lower(u.username) = lower(:login) or lower(u.email) = lower(:login)")
    Optional<User> findByLogin(@Param("login") String login);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
