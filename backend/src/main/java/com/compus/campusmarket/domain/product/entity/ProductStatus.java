package com.compus.campusmarket.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {
    SELLING("판매중"),
    RESERVED("예약중"),
    SOLD_OUT("판매완료");

    private final String description;
}