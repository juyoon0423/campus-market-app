package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus; // 추가
import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(String keyword, String category, ProductStatus status);

    // 내 상품 조회를 위한 메서드 추가
    List<Product> findMyProducts(Long userId);
}