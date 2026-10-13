package com.compus.campusmarket.domain.product.entity;

import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
        // 목록/검색이 status로 필터링하고 created_at으로 정렬하는 게 가장 빈번한 쿼리 패턴이라
        // 두 컬럼을 묶은 복합 인덱스를 둔다(정렬까지 인덱스로 커버).
        @Index(name = "idx_product_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_product_category", columnList = "category")
})
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

    @Column(nullable = false)
    private String category;

    // 거래 희망 장소(카카오맵에서 선택). 이 기능이 생기기 전에 등록된 상품은 null일 수 있다.
    private String tradeLocationName;
    private Double tradeLatitude;
    private Double tradeLongitude;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.SELLING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // 조회수는 ProductRepository.increaseViewCount(벌크 UPDATE)로만 올린다. 엔티티 UPDATE는 전 컬럼을 쓰기 때문에
    // updatable = false로 빼두지 않으면 좋아요·수정 등 다른 트랜잭션이 읽어둔 옛 조회수로 덮어써 조회수가 유실된다.
    @Column(nullable = false, updatable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private int likeCount = 0;

    @Version
    private Long version;

    // 생성
    public static Product create(String title, String description, Long price, User seller, String category) {
        Product product = new Product();
        product.title = title;
        product.description = description;
        product.price = price;
        product.seller = seller;
        product.category = category;
        return product;
    }

    // 수정 (텍스트 정보만)
    public void update(String title, String description, Long price, String category) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public void updateTradeLocation(String tradeLocationName, Double tradeLatitude, Double tradeLongitude) {
        this.tradeLocationName = tradeLocationName;
        this.tradeLatitude = tradeLatitude;
        this.tradeLongitude = tradeLongitude;
    }

    public void validateSeller(Long userId) {
        if (!this.seller.getId().equals(userId)) {
            throw new IllegalStateException("해당 상품에 대한 권한이 없습니다.");
        }
    }

    public void changeStatus(ProductStatus newStatus, Long userId) {
        validateSeller(userId);
        if (this.status == ProductStatus.SOLD_OUT) {
            throw new IllegalStateException("이미 거래가 완료된 상품은 상태를 변경할 수 없습니다.");
        }
        this.status = newStatus;
    }

    public void completeTrade(User buyer, Long userId) {
        validateSeller(userId);
        if (this.status == ProductStatus.SOLD_OUT) {
            throw new IllegalStateException("이미 거래가 완료된 상품입니다.");
        }
        this.status = ProductStatus.SOLD_OUT;
        this.buyer = buyer;
    }

    // --- 비즈니스 로직 추가 ---
    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount--;
    }
}