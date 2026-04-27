package com.compus.campusmarket.domain.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductUpdateRequest {
    private String title;
    private String description;
    private Long price;
}