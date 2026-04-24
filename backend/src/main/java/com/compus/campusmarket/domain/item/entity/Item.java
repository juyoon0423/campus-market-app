// domain/item/entity/Item.java
package com.compus.campusmarket.domain.item.entity;

import com.campusmarket.global.common.BaseTimeEntity;
import com.compus.campusmarket.domain.category.entity.Category;
import com.compus.campusmarket.domain.trade.entity.TradeLocation;
import com.compus.campusmarket.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"seller", "images"})
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob                                 // 긴 텍스트 (TEXT 타입으로 매핑)
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.SELLING;

    // 연관관계 - N:1
    @ManyToOne(fetch = FetchType.LAZY)   // 기본은 항상 LAZY! EAGER는 N+1 문제 유발
    @JoinColumn(name = "user_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_location_id")
    private TradeLocation tradeLocation;

    // 연관관계 - 1:N (사진 분리)
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImage> images = new ArrayList<>();

    // 정적 팩토리 메서드
    public static Item create(String title, String description, int price,
                              User seller, Category category, TradeLocation tradeLocation) {
        Item item = new Item();
        item.title = title;
        item.description = description;
        item.price = price;
        item.seller = seller;
        item.category = category;
        item.tradeLocation = tradeLocation;
        return item;
    }

    // 비즈니스 메서드
    public void markAsSold() {
        this.status = ItemStatus.SOLD;
    }

    public void update(String title, String description, int price) {
        this.title = title;
        this.description = description;
        this.price = price;
    }

    // 연관관계 편의 메서드 (양방향 동기화)
    public void addImage(ItemImage image) {
        this.images.add(image);
        image.assignItem(this);
    }
}