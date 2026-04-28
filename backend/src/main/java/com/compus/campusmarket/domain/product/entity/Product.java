package com.compus.campusmarket.domain.product.entity;

import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long price;

    // 카테고리 필드 추가
    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.SELLING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User seller;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // 생성 메서드 수정 (category 파라미터 추가)
    public static Product create(String title, String description, Long price, User seller, String category) {
        Product product = new Product();
        product.title = title;
        product.description = description;
        product.price = price;
        product.seller = seller;
        product.category = category; // 추가
        return product;
    }

    // 수정 메서드 수정 (category 포함 가능)
    public void update(String title, String description, Long price, String category) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category; // 추가
    }

    // 본인 확인 로직
    public void validateSeller(Long userId) {
        if (!this.seller.getId().equals(userId)) {
            throw new IllegalStateException("해당 상품에 대한 권한이 없습니다.");
        }
    }

    public void changeStatus(ProductStatus newStatus, Long userId) {
        validateSeller(userId); // 본인 확인
        this.status = newStatus;
    }
}