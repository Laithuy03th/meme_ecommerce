package com.example.MyWeb.repository;

import com.example.MyWeb.model.ChatbotKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotKnowledgeRepository extends JpaRepository<ChatbotKnowledge, Long> {

    Optional<ChatbotKnowledge> findByIntent(String intent);

    List<ChatbotKnowledge> findByIsActiveTrueOrderByPriorityDesc();

    List<ChatbotKnowledge> findByRequiresDataTrue();
}
