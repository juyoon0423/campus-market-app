package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
public interface ProductRepositoryCustom {
    List<Product> searchProducts(String keyword, String category, ProductStatus status, Pageable pageable);
    List<Product> findActiveProducts(); // ✅ 추가
    List<Product> findMyProducts(Long userId);
}