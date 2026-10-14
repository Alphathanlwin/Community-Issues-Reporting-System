package com.uit.scirs.user.repository;

import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByNrcNumber(String nrcNumber);

    List<User> findByAccountStatus(AccountStatus accountStatus);

    @EntityGraph(attributePaths = {"role"})
    List<User> findByRoleName(RoleName roleName);

    @EntityGraph(attributePaths = {"role"})
    List<User> findByRoleNameAndAccountStatus(RoleName roleName, AccountStatus accountStatus);

    @EntityGraph(attributePaths = {"role"})
    List<User> findByRoleNameAndAccountStatusIn(RoleName roleName, List<AccountStatus> accountStatuses);

    @EntityGraph(attributePaths = {"role"})
    List<User> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"role"})
    List<User> findTop10ByRoleNameOrderByCreatedAtDesc(RoleName roleName);

    @EntityGraph(attributePaths = {"role"})
    List<User> findTop10ByRoleNameAndAccountStatusOrderByCreatedAtDesc(RoleName roleName, AccountStatus accountStatus);

    long countByAccountStatus(AccountStatus accountStatus);

    long countByRoleName(RoleName roleName);

    long countByRoleNameAndAccountStatusIn(RoleName roleName, List<AccountStatus> accountStatuses);

    // Leaderboard ranking: highest score first, ties broken by earliest join date.
    @EntityGraph(attributePaths = {"role"})
    List<User> findByRoleNameOrderByScorePointsDescCreatedAtAsc(RoleName roleName, Pageable pageable);
}
