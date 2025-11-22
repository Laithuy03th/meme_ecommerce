package com.example.MyWeb.repository;

import com.example.MyWeb.model.CustomerProfile;
import com.example.MyWeb.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByUser(User user);
}
