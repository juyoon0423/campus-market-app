package com.compus.campusmarket.domain.product.entity;

import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class ProductLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductLike(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}