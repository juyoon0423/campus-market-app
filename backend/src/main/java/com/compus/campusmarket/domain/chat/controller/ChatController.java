package com.compus.campusmarket.domain.chat.controller;

import com.compus.campusmarket.domain.chat.dto.ChatMessageRequest;
import com.compus.campusmarket.domain.chat.dto.ChatMessageResponse;
import com.compus.campusmarket.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    // 클라이언트가 /pub/chat/message로 메시지를 보내면 호출됨
    // senderId는 요청 바디가 아니라 CONNECT 시 StompAuthChannelInterceptor가 검증해 심어둔
    // Principal에서 가져온다(클라이언트가 임의로 다른 사용자 명의를 지정하지 못하도록).
    @MessageMapping("/chat/message")
    public void message(ChatMessageRequest request, Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        ChatMessageResponse response = chatService.saveMessage(request, senderId);
        // /sub/chat/room/{roomId}를 구독 중인 유저들에게 저장된(서버가 확정한) 메시지를 전달
        messagingTemplate.convertAndSend("/sub/chat/room/" + response.getRoomId(), response);
    }
}