package com.compus.campusmarket.domain.chat.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter @Setter
public class ChatMessageRequest {
    private Long roomId;
    private Long senderId;
    private String message;
}