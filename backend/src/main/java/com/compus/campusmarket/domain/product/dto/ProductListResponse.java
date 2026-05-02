package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import lombok.Getter;

@Getter
public class ProductListResponse {
    private Long id;
    private String title;
    private Long price;
    private String sellerName;
    private String representativeImageUrl; // 첫 번째 사진
    private ProductStatus status; // ✅ 상태 필드 추가

    public ProductListResponse(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.price = product.getPrice();
        this.sellerName = product.getSeller().getName();
        this.status = product.getStatus(); // ✅ 상태 정보 추가
        // 이미지가 있으면 첫 번째 이미지 경로를, 없으면 null 반환
        this.representativeImageUrl = product.getImages().isEmpty() ?
                null : product.getImages().get(0).getImageUrl();
    }
}