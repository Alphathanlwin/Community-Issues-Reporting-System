package com.uit.scirs.user.repository;

import com.uit.scirs.user.entity.AccountStatus;
import com.uit.scirs.user.entity.RoleName;
import com.uit.scirs.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByNrcNumber(String nrcNumber);

    List<User> findByAccountStatus(AccountStatus accountStatus);

    List<User> findByRoleName(RoleName roleName);

    List<User> findByRoleNameAndAccountStatus(RoleName roleName, AccountStatus accountStatus);

    List<User> findByDepartmentId(Long departmentId);

    List<User> findTop10ByRoleNameOrderByCreatedAtDesc(RoleName roleName);

    long countByAccountStatus(AccountStatus accountStatus);

    // Leaderboard ranking: highest score first, ties broken by earliest join date.
    List<User> findByRoleNameOrderByScorePointsDescCreatedAtAsc(RoleName roleName, Pageable pageable);
}
