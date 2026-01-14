package com.example.MyWeb.repository;

import com.example.MyWeb.model.RefreshToken;
import com.example.MyWeb.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser(User user);

    void deleteByUser(User user);

    void deleteByExpiresAtBeforeOrRevokedTrue(LocalDateTime now);
}
