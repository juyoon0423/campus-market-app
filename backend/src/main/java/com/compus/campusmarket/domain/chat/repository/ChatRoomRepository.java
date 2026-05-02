package com.compus.campusmarket.domain.chat.repository;

import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // ✅ 리스트로 반환하도록 수정
    List<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    List<ChatRoom> findAllBySellerIdOrBuyerId(Long sellerId, Long buyerId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.product.id = :productId AND (cr.seller.id = :userId OR cr.buyer.id = :userId)")
    List<ChatRoom> findByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);
}