package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ProductDetailResponse {
    private Long id;  // 이 필드 추가 필요
    private String title;
    private String description;
    private Long price;
    private String category;  // 이 필드 추가 필요
    private String sellerName;
    private Long sellerId;  // 이 필드 추가 필요
    private double sellerTrustScore;
    private ProductStatus status;  // 이 필드 추가
    private List<String> imageUrls;

    public ProductDetailResponse(Product product) {
        this.id = product.getId();  // 이 라인 추가
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.category = product.getCategory();  // 이 라인 추가
        this.sellerName = product.getSeller().getName();
        this.sellerId = product.getSeller().getId();  // 이 라인 추가 필요
        this.sellerTrustScore = product.getSeller().getTrustScore();
        this.status = product.getStatus();  // 이 라인 추가
        this.imageUrls = product.getImages().stream()
                .map(img -> img.getImageUrl())
                .collect(Collectors.toList());
    }
}