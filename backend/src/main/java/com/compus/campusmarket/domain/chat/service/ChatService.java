package com.compus.campusmarket.domain.chat.service;

import com.compus.campusmarket.domain.chat.dto.ChatMessageRequest;
import com.compus.campusmarket.domain.chat.dto.ChatMessageResponse;
import com.compus.campusmarket.domain.chat.dto.ChatRoomResponse;
import com.compus.campusmarket.domain.chat.entity.ChatMessage;
import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import com.compus.campusmarket.domain.chat.repository.ChatMessageRepository;
import com.compus.campusmarket.domain.chat.repository.ChatRoomRepository;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomResponse createOrGetRoom(Long productId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        if (product.getSeller().getId().equals(buyerId)) {
            throw new IllegalStateException("본인 상품에는 채팅을 시작할 수 없습니다.");
        }

        List<ChatRoom> existingRooms = chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId);
        ChatRoom room;

        if (!existingRooms.isEmpty()) {
            room = existingRooms.get(0);
        } else {
            User buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new IllegalArgumentException("구매자 정보가 올바르지 않습니다."));
            ChatRoom newRoom = ChatRoom.builder()
                    .product(product)
                    .seller(product.getSeller())
                    .buyer(buyer)
                    .build();
            room = chatRoomRepository.save(newRoom);
        }

        String lastMessage = getLastMessage(room.getId());
        // 서비스에서 DTO로 변환해서 반환
        return new ChatRoomResponse(room, buyerId, lastMessage);
    }
    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderId(request.getSenderId())
                .message(request.getMessage())
                .build();

        return new ChatMessageResponse(chatMessageRepository.save(message));
    }

    // 🚨 핵심: 리스트 조회를 서비스에서 DTO로 변환
    public List<ChatRoomResponse> findAllRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findAllBySellerIdOrBuyerId(userId, userId);
        Map<Long, String> lastMessages = findLastMessages(rooms);
        return rooms.stream()
                .map(room -> new ChatRoomResponse(room, userId, lastMessages.get(room.getId())))
                .collect(Collectors.toList());
    }

    // 방 개수만큼 getLastMessage를 반복 호출하던 N+1을 없애고 단일 쿼리로 방별 마지막 메시지를 가져온다.
    private Map<Long, String> findLastMessages(List<ChatRoom> rooms) {
        if (rooms.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).collect(Collectors.toList());
        return chatMessageRepository.findLastMessagesByRoomIds(roomIds).stream()
                .collect(Collectors.toMap(
                        ChatMessageRepository.LastMessageRow::getRoomId,
                        ChatMessageRepository.LastMessageRow::getMessage));
    }

    public List<ChatMessageResponse> findMessagesByRoomId(Long roomId) {
        return chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    // 마지막 메시지 조회 메서드 추가
    public String getLastMessage(Long roomId) {
        return chatMessageRepository.findLastMessageByRoomId(roomId).orElse(null);
    }

    // ChatService에 추가
    public List<ChatRoomResponse> findRoomsByProductId(Long productId, Long currentUserId) {
        List<ChatRoom> rooms = chatRoomRepository.findByProductIdAndUserId(productId, currentUserId);
        Map<Long, String> lastMessages = findLastMessages(rooms);
        return rooms.stream()
                .map(room -> new ChatRoomResponse(room, currentUserId, lastMessages.get(room.getId())))
                .collect(Collectors.toList());
    }
}