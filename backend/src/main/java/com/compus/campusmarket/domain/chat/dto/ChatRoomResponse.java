package com.compus.campusmarket.domain.chat.dto;

import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomResponse {
    private Long id;                // 채팅방 ID
    private Long buyerId;           // 구매자 ID
    private String opponentName;    // 상대방 이름
    private String productName;     // 상품명
    private String lastMessage;     // 마지막 메시지

    // 생성자 수정 - lastMessage 파라미터 추가
    public ChatRoomResponse(ChatRoom entity, Long currentUserId, String lastMessage) {
        this.id = entity.getId();
        this.productName = entity.getProduct().getTitle();

        // 로그인한 유저가 구매자라면 상대방은 판매자, 반대라면 구매자
        if (entity.getBuyer().getId().equals(currentUserId)) {
            this.opponentName = entity.getSeller().getName();
        } else {
            this.opponentName = entity.getBuyer().getName();
        }

        this.buyerId = entity.getBuyer().getId();
        this.lastMessage = lastMessage != null ? lastMessage : "대화 내용이 없습니다.";
    }
}