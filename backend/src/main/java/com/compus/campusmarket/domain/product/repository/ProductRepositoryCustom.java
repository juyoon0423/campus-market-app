package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus; // 추가
import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(String keyword, String category, ProductStatus status);
    List<Product> findActiveProducts(); // ✅ 추가
    List<Product> findMyProducts(Long userId);
}