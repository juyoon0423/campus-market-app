package com.compus.campusmarket.domain.product.review.entity;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private User writer; // 작성자 (구매자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private User target; // 대상자 (판매자)

    @Column(nullable = false)
    private double rating; // 별점 (1.0 ~ 5.0)

    @Column(columnDefinition = "TEXT")
    private String content;

    public static Review create(Product product, User writer, User target, double rating, String content) {
        Review review = new Review();
        review.product = product;
        review.writer = writer;
        review.target = target;
        review.rating = rating;
        review.content = content;
        return review;
    }
}