package com.compus.campusmarket.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ProductUpdateRequest {
    @NotBlank(message = "상품명을 입력해주세요.")
    private String title;

    @NotBlank(message = "상품 설명을 입력해주세요.")
    private String description;

    @NotNull(message = "가격을 입력해주세요.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private Long price;

    @NotBlank(message = "카테고리를 선택해주세요.")
    private String category;

    // 기존 이미지 중 삭제하지 않고 남겨둘 이미지 URL 목록
    private List<String> remainingImageUrls = new ArrayList<>();
}