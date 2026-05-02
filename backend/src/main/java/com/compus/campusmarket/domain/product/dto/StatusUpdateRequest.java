package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.ProductStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    private ProductStatus status;
    private Long buyerId;  // SOLD_OUT일 때 필요
}