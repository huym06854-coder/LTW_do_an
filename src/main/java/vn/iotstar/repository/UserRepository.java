package vn.iotstar.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.*;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Override
    @EntityGraph(attributePaths = {"role", "branch"})
    Page<User> findAll(org.springframework.data.jpa.domain.Specification<User> spec, Pageable pageable);

    @Query("select u.branch.id, u.role.name, count(u) from User u where u.branch is not null and u.role.name in :roles group by u.branch.id, u.role.name")
    List<Object[]> countEmployeesByBranch(@Param("roles") Collection<UserRole> roles);

    @EntityGraph(attributePaths = {"role", "branch"})
    Page<User> findByBranchIdAndRoleNameIn(Long branchId, Collection<UserRole> roles, Pageable pageable);
    @Query("select u from User u join fetch u.role r where u.status = :active and (r.name = :admin or (u.branch.id = :branchId and r.name in :roles))")
    List<User> findOrderOperators(@Param("branchId") Long branchId, @Param("active") UserStatus active,
            @Param("admin") UserRole admin, @Param("roles") Collection<UserRole> roles);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockForAddressBook(@Param("id") Long id);

    @EntityGraph(attributePaths = {"role", "branch"})
    Optional<User> findWithRoleById(Long id);

    @EntityGraph(attributePaths = {"role", "branch"})
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select u from User u where lower(u.username) = lower(:login) or lower(u.email) = lower(:login)")
    Optional<User> findByLogin(@Param("login") String login);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
