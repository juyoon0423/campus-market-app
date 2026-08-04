package com.compus.campusmarket.domain.chat.repository;

import com.compus.campusmarket.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByChatRoomIdOrderByCreatedAtAsc(Long roomId);

    // derived delete는 엔티티를 하나씩 remove()하기 때문에 호출부에 트랜잭션이 없으면 실패한다 — 명시적으로 걸어둔다.
    @Transactional
    void deleteAllByChatRoom_IdIn(List<Long> chatRoomIds);
    // 마지막 메시지 조회 쿼리 추가
    @Query("SELECT cm.message FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId ORDER BY cm.createdAt DESC LIMIT 1")
    Optional<String> findLastMessageByRoomId(@Param("roomId") Long roomId);

    // 채팅방 목록 조회 시 방마다 findLastMessageByRoomId를 반복 호출하던 N+1을 없애기 위해
    // 방 ID 목록을 한 번에 받아 방별 마지막 메시지를 단일 쿼리로 가져온다.
    @Query(value = "SELECT cm.chat_room_id AS roomId, cm.message AS message " +
            "FROM chat_message cm " +
            "INNER JOIN (" +
            "  SELECT chat_room_id, MAX(id) AS max_id " +
            "  FROM chat_message " +
            "  WHERE chat_room_id IN (:roomIds) " +
            "  GROUP BY chat_room_id" +
            ") latest ON cm.chat_room_id = latest.chat_room_id AND cm.id = latest.max_id",
            nativeQuery = true)
    List<LastMessageRow> findLastMessagesByRoomIds(@Param("roomIds") List<Long> roomIds);

    interface LastMessageRow {
        Long getRoomId();
        String getMessage();
    }
}