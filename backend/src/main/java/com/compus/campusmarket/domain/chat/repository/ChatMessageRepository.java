package com.compus.campusmarket.domain.chat.repository;

import com.compus.campusmarket.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByChatRoomIdOrderByCreatedAtAsc(Long roomId);
    // 마지막 메시지 조회 쿼리 추가
    @Query("SELECT cm.message FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId ORDER BY cm.createdAt DESC LIMIT 1")
    Optional<String> findLastMessageByRoomId(@Param("roomId") Long roomId);



}