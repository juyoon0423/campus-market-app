package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;


@Getter
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
                null : product.getImages().get(0).getImageUrl();
    }
}