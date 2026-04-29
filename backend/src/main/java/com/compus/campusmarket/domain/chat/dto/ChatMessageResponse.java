package com.compus.campusmarket.domain.chat.dto;

import com.compus.campusmarket.domain.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String message;
    private LocalDateTime createdAt;

    public ChatMessageResponse(ChatMessage entity) {
        this.id = entity.getId();
        this.roomId = entity.getChatRoom().getId();
        this.senderId = entity.getSenderId();
        this.message = entity.getMessage();
        this.createdAt = entity.getCreatedAt();
    }
}