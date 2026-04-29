package com.compus.campusmarket.domain.chat.controller;

import com.compus.campusmarket.domain.chat.dto.ChatMessageResponse;
import com.compus.campusmarket.domain.chat.dto.ChatRoomResponse;
import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import com.compus.campusmarket.domain.chat.service.ChatService;
import com.compus.campusmarket.global.config.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    // 채팅방 생성 또는 기존 방 조회
    @PostMapping("/room/{productId}")
    public ResponseEntity<ChatRoomResponse> createOrGetRoom(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        ChatRoom room = chatService.createOrGetRoom(productId, currentUserId);
        return ResponseEntity.ok(new ChatRoomResponse(room, currentUserId));
    }

    // 내 채팅방 리스트 조회
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        List<ChatRoomResponse> responses = chatService.findAllRooms(currentUserId)
                .stream()
                .map(room -> new ChatRoomResponse(room, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // 채팅방 메시지 내역 조회
    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getRoomMessages(@PathVariable Long roomId) {
        List<ChatMessageResponse> responses = chatService.findMessagesByRoomId(roomId)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}