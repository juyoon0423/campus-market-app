package com.compus.campusmarket.domain.chat.repository;

import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);
    List<ChatRoom> findAllBySellerIdOrBuyerId(Long sellerId, Long buyerId);
}