package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // N+1 문제 해결: seller를 페치 조인하여 한 번의 쿼리로 가져옴
    @Query("select p from Product p join fetch p.seller order by p.createdAt desc")
    List<Product> findAllWithSeller();
}