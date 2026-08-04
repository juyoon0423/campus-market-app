package com.compus.campusmarket.domain.chat.repository;

import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // ✅ 리스트로 반환하도록 수정
    List<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    List<ChatRoom> findAllBySellerIdOrBuyerId(Long sellerId, Long buyerId);

    List<ChatRoom> findAllByProduct_Id(Long productId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.product.id = :productId AND (cr.seller.id = :userId OR cr.buyer.id = :userId)")
    List<ChatRoom> findByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);

    // derived delete는 엔티티를 하나씩 remove()하기 때문에 호출부에 트랜잭션이 없으면 실패한다 — 명시적으로 걸어둔다.
    @Transactional
    void deleteAllByProduct_Id(Long productId);
}