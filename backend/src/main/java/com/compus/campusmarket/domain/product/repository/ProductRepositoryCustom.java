package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(String keyword, String category);
}