package com.example.MyWeb.repository;

import com.example.MyWeb.model.Address;
import com.example.MyWeb.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser(User user);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    List<Address> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Transactional
    @Modifying
    @Query("update Address a set a.isDefault = false where a.user.id = :userId")
    void clearDefaultByUserId(Long userId);
}
