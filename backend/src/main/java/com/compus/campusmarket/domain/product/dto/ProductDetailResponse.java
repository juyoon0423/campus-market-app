package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductImage;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
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

    public ProductDetailResponse(Product product) {
        this.id = product.getId();
        this.title = product.getTitle();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.category = product.getCategory();
        this.sellerName = product.getSeller().getName();
        this.sellerId = product.getSeller().getId();
        this.sellerTrustScore = product.getSeller().getTrustScore();
        this.status = product.getStatus();
        this.imageUrls = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());
    }
}