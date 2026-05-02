package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    // seller와 images를 모두 페치 조인으로 한 번에 가져옴
    @Query("select distinct p from Product p " +
            "join fetch p.seller " +
            "left join fetch p.images " + // 이미지가 없을 수도 있으므로 left join
            "order by p.createdAt desc")
    List<Product> findAllWithSellerAndImages();

    @Query("select p from Product p join fetch p.seller left join fetch p.images where p.id = :productId")
    Optional<Product> findByIdWithSeller(@Param("productId") Long productId);
}