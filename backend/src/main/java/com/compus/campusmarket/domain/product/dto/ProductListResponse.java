package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // ✅ 1. Redis JSON 역직렬화를 위한 빈 껍데기 생성자 (필수!)
public class ProductListResponse {
    private Long id;
    private String title;
    private Long price;
    private String sellerName;
    private String representativeImageUrl;
    private ProductStatus status;
    private int viewCount;
    private int likeCount;

    @JsonProperty("isLiked")
    private boolean isLiked;

    // 기존에 만드신 생성자 (DB에서 꺼내올 때 사용, 그대로 유지!)
    @JsonIgnore
    public ProductListResponse(Product product, boolean isLiked) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.price = product.getPrice();
        this.sellerName = product.getSeller().getName();
        this.status = product.getStatus();
        this.viewCount = product.getViewCount();
        this.likeCount = product.getLikeCount();
        this.isLiked = isLiked;
        this.representativeImageUrl = product.getImages().isEmpty() ?
                null : "/images/" + product.getImages().get(0).getImageUrl();
    }
}