package com.compus.campusmarket.domain.trade.entity;

import com.compus.campusmarket.domain.item.entity.Item;
import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trade_location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeLocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String landmark;             // 랜드마크 이름 (예: 도서관 앞, 학생회관)

    @Column(length = 200)
    private String description;          // 상세 설명

    @OneToMany(mappedBy = "tradeLocation")
    private List<Item> items = new ArrayList<>();

    public static TradeLocation create(String landmark, String description) {
        TradeLocation location = new TradeLocation();
        location.landmark = landmark;
        location.description = description;
        return location;
    }
}