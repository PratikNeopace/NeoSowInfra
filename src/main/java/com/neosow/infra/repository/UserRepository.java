package com.neosow.infra.repository;

import com.neosow.infra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import com.neosow.infra.model.ERole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<User> findByParentAdminId(UUID parentAdminId, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Page<User> findAllByRoleName(@Param("roleName") ERole roleName, Pageable pageable);

    @Query("SELECT u.email FROM User u WHERE u.parentAdminId = :adminId")
    List<String> findEmailsByParentAdminId(@Param("adminId") UUID adminId);

    long countByParentAdminId(UUID parentAdminId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") ERole roleName);
}
