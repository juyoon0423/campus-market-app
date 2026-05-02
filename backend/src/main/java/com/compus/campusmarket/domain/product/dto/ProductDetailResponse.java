package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ProductDetailResponse {
    private String title;
    private String description;
    private Long price;
    private String sellerName;
    private Long sellerId;  // 이 필드 추가 필요
    private double sellerTrustScore;
    private List<String> imageUrls;

    public ProductDetailResponse(Product product) {
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.sellerName = product.getSeller().getName();
        this.sellerId = product.getSeller().getId();  // 이 라인 추가 필요
        this.sellerTrustScore = product.getSeller().getTrustScore();
        this.imageUrls = product.getImages().stream()
                .map(img -> img.getImageUrl())
                .collect(Collectors.toList());
    }
}