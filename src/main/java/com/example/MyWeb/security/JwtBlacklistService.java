package com.example.MyWeb.security;

import com.example.MyWeb.model.BlacklistedToken;
import com.example.MyWeb.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final BlacklistedTokenRepository repository;

    public void blacklist(String token, long expEpochSeconds) {
        LocalDateTime expiresAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(expEpochSeconds),
                ZoneId.systemDefault());
        repository.save(new BlacklistedToken(token, expiresAt));
    }

    public boolean isBlacklisted(String token) {
        return repository.findById(token)
                .map(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}
