package com.compus.campusmarket.domain.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductCreateRequest {
    private String title;
    private String description;
    private Long price;
}