package com.example.MyWeb.repository;

import com.example.MyWeb.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ChatMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Long countBySessionId(String sessionId);
}
