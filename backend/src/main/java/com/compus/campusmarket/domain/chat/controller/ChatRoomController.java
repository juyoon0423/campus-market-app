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
        String lastMessage = chatService.getLastMessage(room.getId());
        return ResponseEntity.ok(new ChatRoomResponse(room, currentUserId, lastMessage));
    }

    // 내 채팅방 리스트 조회
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        List<ChatRoom> rooms = chatService.findAllRooms(currentUserId);

        List<ChatRoomResponse> responses = rooms.stream()
                .map(room -> {
                    String lastMessage = chatService.getLastMessage(room.getId());
                    return new ChatRoomResponse(room, currentUserId, lastMessage);
                })
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

    // 특정 상품의 채팅방 목록 조회
    @GetMapping("/rooms/product/{productId}")
    public ResponseEntity<List<ChatRoomResponse>> getProductRooms(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        List<ChatRoom> rooms = chatService.findRoomsByProductId(productId, currentUserId);

        List<ChatRoomResponse> responses = rooms.stream()
                .map(room -> {
                    String lastMessage = chatService.getLastMessage(room.getId());
                    return new ChatRoomResponse(room, currentUserId, lastMessage);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}