package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import lombok.Getter;

@Getter
public class ProductListResponse {
    private Long id;
    private String title;
    private Long price;
    private String sellerName;
    private String representativeImageUrl; // 첫 번째 사진

    public ProductListResponse(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.price = product.getPrice();
        this.sellerName = product.getSeller().getName();
        // 이미지가 있으면 첫 번째 이미지 경로를, 없으면 null 반환
        this.representativeImageUrl = product.getImages().isEmpty() ?
                null : product.getImages().get(0).getImageUrl();
    }
}