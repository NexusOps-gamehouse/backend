package com.example.gamehouse.repository;

import com.example.gamehouse.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}