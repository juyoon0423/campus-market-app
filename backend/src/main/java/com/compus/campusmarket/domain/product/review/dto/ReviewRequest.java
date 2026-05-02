package com.compus.campusmarket.domain.product.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequest {
    private double rating;
    private String content;
}