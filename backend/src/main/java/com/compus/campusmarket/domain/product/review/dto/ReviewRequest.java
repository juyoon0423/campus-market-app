package com.compus.campusmarket.domain.product.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequest {
    @DecimalMin(value = "1.0", message = "별점은 1.0 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다.")
    private double rating;

    @NotBlank(message = "리뷰 내용을 입력해주세요.")
    private String content;
}