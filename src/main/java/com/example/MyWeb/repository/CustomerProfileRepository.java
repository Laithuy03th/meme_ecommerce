package com.example.MyWeb.repository;

import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByUser(User user);

    // Load profile theo danh sách userId trong 1 query (tránh N+1)
    @Query("SELECT cp FROM CustomerProfile cp WHERE cp.user.id IN :userIds")
    List<CustomerProfile> findByUserIdIn(@Param("userIds") List<Long> userIds);
}
