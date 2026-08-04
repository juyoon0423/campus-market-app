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
        // 서비스가 DTO를 주므로 그대로 리턴!
        ChatRoomResponse response = chatService.createOrGetRoom(productId, currentUserId);
        return ResponseEntity.ok(response);
    }

    // 내 채팅방 리스트 조회
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        // 서비스가 DTO 리스트를 주므로 그대로 리턴!
        List<ChatRoomResponse> responses = chatService.findAllRooms(currentUserId);
        return ResponseEntity.ok(responses);
    }

    // 채팅방 메시지 내역 조회
    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getRoomMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        // 서비스가 DTO 리스트를 주므로 그대로 리턴!
        List<ChatMessageResponse> responses = chatService.findMessagesByRoomId(roomId, currentUserId);
        return ResponseEntity.ok(responses);
    }

    // 특정 상품의 채팅방 목록 조회
    @GetMapping("/rooms/product/{productId}")
    public ResponseEntity<List<ChatRoomResponse>> getProductRooms(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getUserId();
        // 서비스가 DTO 리스트를 주므로 그대로 리턴!
        List<ChatRoomResponse> responses = chatService.findRoomsByProductId(productId, currentUserId);
        return ResponseEntity.ok(responses);
    }
}