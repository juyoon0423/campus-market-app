package com.compus.campusmarket.domain.chat.controller;

import com.compus.campusmarket.domain.chat.dto.ChatMessageRequest;
import com.compus.campusmarket.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    // 클라이언트가 /pub/chat/message로 메시지를 보내면 호출됨
    @MessageMapping("/chat/message")
    public void message(ChatMessageRequest message) {
        chatService.saveMessage(message);
        // /sub/chat/room/{roomId}를 구독 중인 유저들에게 메시지 전달
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }
}