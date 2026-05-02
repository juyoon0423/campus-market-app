package com.compus.campusmarket.domain.product.review.dto;

import com.compus.campusmarket.domain.product.entity.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StatusUpdateRequest {
    private ProductStatus status;
    private Long buyerId; // SOLD_OUT 시에만 사용
}