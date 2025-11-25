package com.example.MyWeb.repository;

import org.springframework.data.domain.Pageable;
import com.example.MyWeb.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    // ====== thêm cho Admin User ======

    // List tất cả user, sort theo createdAt mới nhất
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Filter theo status (ACTIVE/BLOCKED)
    Page<User> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // Filter theo role
    Page<User> findDistinctByRoles_CodeOrderByCreatedAtDesc(String roleCode, Pageable pageable);

    // Dashboard
    Long countByCreatedAtAfter(java.time.LocalDateTime createdAt);

    // Filter theo cả status + role
    Page<User> findDistinctByStatusAndRoles_CodeOrderByCreatedAtDesc(String status, String roleCode, Pageable pageable);
}
