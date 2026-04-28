package com.compus.campusmarket.domain.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductUpdateRequest {
    private String title;
    private String description;
    private Long price;
    private String category; // 이 필드가 반드시 있어야 합니다.
}