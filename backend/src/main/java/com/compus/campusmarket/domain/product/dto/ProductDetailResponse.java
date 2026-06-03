package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductImage;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ProductDetailResponse {
    private Long id;
    private String title;
    private String description;
    private Long price;
    private String category;
    private String sellerName;
    private Long sellerId;
    private double sellerTrustScore;
    private ProductStatus status;
    private List<String> imageUrls;
    private int viewCount;
    private int likeCount;
    @JsonProperty("isLiked")
    private boolean isLiked; // 현재 로그인한 사용자가 좋아요를 눌렀는지 여부

    public ProductDetailResponse(Product product, boolean isLiked) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.category = product.getCategory();
        this.sellerName = product.getSeller().getName();
        this.sellerId = product.getSeller().getId();
        this.sellerTrustScore = product.getSeller().getTrustScore();
        this.status = product.getStatus();
        this.viewCount = product.getViewCount();
        this.likeCount = product.getLikeCount();
        this.isLiked = isLiked;
        this.imageUrls = product.getImages().stream()
                .map(image -> "/images/" + image.getImageUrl())
                .collect(Collectors.toList());
    }
}