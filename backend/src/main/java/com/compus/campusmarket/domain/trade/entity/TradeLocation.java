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
public class TradeLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String address;       // 도로명 주소 또는 지번 주소

    @Column(nullable = false)
    private Double latitude;      // 위도 (예: 37.123456)

    @Column(nullable = false)
    private Double longitude;     // 경도 (예: 127.123456)

    private String landmark;      // 사용자가 지정한 장소 명칭 (예: 미래관 앞)

    private String description;   // 상세 설명 (예: 1층 자판기 옆에서 만나요)

    @Builder
    public TradeLocation(String address, Double latitude, Double longitude, String landmark, String description) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.landmark = landmark;
        this.description = description;
    }
}