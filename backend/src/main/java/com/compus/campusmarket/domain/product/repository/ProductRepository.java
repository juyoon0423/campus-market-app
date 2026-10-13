package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    // 비관적 락: 동시 요청 중 하나가 끝날 때까지 다른 트랜잭션의 조회를 블로킹
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // 조회수는 엔티티 수정(dirty checking)이 아니라 벌크 UPDATE로 올린다. 엔티티로 수정하면 Product.version이
    // 같이 올라가 동시 조회끼리, 또 좋아요(@Version 낙관적 락)와 충돌한다. "update versioned"를 쓰지 않으므로
    // version은 건드리지 않는다. 판매자 본인의 조회(viewerId = seller)는 세지 않는다.
    @Modifying
    @Query("update Product p set p.viewCount = p.viewCount + 1 " +
            "where p.id = :id and (:viewerId is null or p.seller.id <> :viewerId)")
    int increaseViewCount(@Param("id") Long id, @Param("viewerId") Long viewerId);
}