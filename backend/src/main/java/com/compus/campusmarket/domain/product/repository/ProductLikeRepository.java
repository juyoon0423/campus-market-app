package com.compus.campusmarket.domain.product.repository;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductLike;
import com.compus.campusmarket.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {
    Optional<ProductLike> findByUserAndProduct(User user, Product product);

    // 유저 ID와 상품 ID로 좋아요 여부를 빠르게 확인하기 위한 메서드
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    // 상품별 실제 좋아요 row 수 — 동시성 테스트에서 Product.likeCount와의 정합성을 검증하기 위해 사용
    long countByProduct_Id(Long productId);

    // derived delete는 엔티티를 하나씩 remove()하기 때문에 호출부에 트랜잭션이 없으면(테스트 등) 실패한다 — 명시적으로 걸어둔다.
    @Transactional
    void deleteAllByProduct_Id(Long productId);

    // 추가: 내가 찜한 상품 목록 가져오기 (N+1 문제 방지를 위한 페치 조인 적용)
    @Query("select pl.product from ProductLike pl " +
            "join fetch pl.product.seller " +
            "left join fetch pl.product.images " +
            "where pl.user.id = :userId " +
            "order by pl.createdAt desc")
    List<Product> findLikedProductsByUserId(@Param("userId") Long userId);
}