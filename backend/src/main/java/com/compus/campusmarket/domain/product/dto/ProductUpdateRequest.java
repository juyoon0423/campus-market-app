package com.compus.campusmarket.domain.product.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ProductUpdateRequest {
    private String title;
    private String description;
    private Long price;
    private String category;

    // 기존 이미지 중 삭제하지 않고 남겨둘 이미지 URL 목록
    private List<String> remainingImageUrls = new ArrayList<>();
}