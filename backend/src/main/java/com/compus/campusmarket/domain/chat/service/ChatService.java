package com.compus.campusmarket.domain.chat.service;

import com.compus.campusmarket.domain.chat.dto.ChatMessageRequest;
import com.compus.campusmarket.domain.chat.dto.ChatMessageResponse;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoom createOrGetRoom(Long productId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        if (product.getSeller().getId().equals(buyerId)) {
            throw new IllegalStateException("본인 상품에는 채팅을 시작할 수 없습니다.");
        }

        return chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId)
                .orElseGet(() -> {
                    User buyer = userRepository.findById(buyerId).orElseThrow();
                    ChatRoom newRoom = ChatRoom.builder()
                            .product(product)
                            .seller(product.getSeller())
                            .buyer(buyer)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });
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

    public List<ChatRoom> findAllRooms(Long userId) {
        return chatRoomRepository.findAllBySellerIdOrBuyerId(userId, userId);
    }

    public List<ChatMessage> findMessagesByRoomId(Long roomId) {
        return chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(roomId);
    }
}