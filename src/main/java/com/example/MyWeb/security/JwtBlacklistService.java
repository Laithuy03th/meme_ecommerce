package com.example.MyWeb.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo in-memory blacklist. Sản xuất nên dùng Redis.
 */
@Component
public class JwtBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, long expEpochSeconds) {
        blacklist.put(token, expEpochSeconds);
    }

    public boolean isBlacklisted(String token) {
        Long exp = blacklist.get(token);
        if (exp == null)
            return false;
        if (Instant.now().getEpochSecond() > exp) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
