package com.compus.campusmarket.domain.chat.dto;

import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomResponse {
    private Long id;                // 채팅방 ID
    private String opponentName;    // 상대방 이름 (User.name 필드 사용)
    private String productName;     // 상품명
    private String lastMessage;     // 마지막 메시지

    /**
     * Entity -> DTO 변환 생성자
     * @param entity 채팅방 엔티티
     * @param currentUserId 현재 로그인한 사용자 ID (상대방 판별용)
     */
    public ChatRoomResponse(ChatRoom entity, Long currentUserId) {
        this.id = entity.getId();
        this.productName = entity.getProduct().getTitle();

        // 로그인한 유저가 구매자라면 상대방은 판매자(Seller), 반대라면 구매자(Buyer)의 이름을 가져옴
        if (entity.getBuyer().getId().equals(currentUserId)) {
            this.opponentName = entity.getSeller().getName(); // User.name 사용
        } else {
            this.opponentName = entity.getBuyer().getName();  // User.name 사용
        }

        // 마지막 메시지 기본값 설정 (추후 메시지 리스트 연동 시 로직 추가 가능)
        this.lastMessage = "대화 내용이 없습니다.";
    }
}