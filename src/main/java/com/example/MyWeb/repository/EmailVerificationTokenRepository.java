package com.example.MyWeb.repository;

import com.example.MyWeb.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for EmailVerificationToken operations
 * 
 * @author Senior Software Engineer
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find token by token string
     * 
     * @param token the token string
     * @return Optional containing the token if found
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Find active (unverified, non-expired) token for user
     * 
     * @param userId the user ID
     * @param now    current time
     * @return Optional containing the token if found
     */
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user.id = :userId " +
            "AND t.verifiedAt IS NULL AND t.expiresAt > :now")
    Optional<EmailVerificationToken> findActiveTokenByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    /**
     * Delete all tokens for a user
     * 
     * @param userId the user ID
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    /**
     * Delete expired tokens (cleanup job)
     * 
     * @param now current time
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now AND t.verifiedAt IS NULL")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
