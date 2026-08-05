package com.compus.campusmarket.domain.chat.entity;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "buyer_id"}))
public class ChatRoom extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    public void validateParticipant(Long userId) {
        boolean isParticipant = this.seller.getId().equals(userId) || this.buyer.getId().equals(userId);
        if (!isParticipant) {
            throw new IllegalStateException("해당 채팅방에 대한 권한이 없습니다.");
        }
    }
}