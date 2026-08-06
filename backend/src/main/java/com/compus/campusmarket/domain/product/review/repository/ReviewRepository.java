package com.compus.campusmarket.domain.product.review.repository;

import com.compus.campusmarket.domain.product.review.entity.Review;
import com.compus.campusmarket.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 유저(판매자)가 받은 모든 리뷰를 조회 (신뢰 지수 계산용)
    List<Review> findByTarget(User target);

    // 신뢰 지수 재계산용 — 리뷰를 전부 로드해 Java에서 평균 내는 대신 DB에서 바로 집계한다.
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.target = :target")
    Double findAverageRatingByTarget(@Param("target") User target);

    // 특정 상품에 대해 이미 리뷰가 작성되었는지 확인 (중복 작성 방지용)
    boolean existsByProductIdAndWriterId(Long productId, Long writerId);

    // derived delete는 엔티티를 하나씩 remove()하기 때문에 호출부에 트랜잭션이 없으면 실패한다 — 명시적으로 걸어둔다.
    @Transactional
    void deleteAllByProduct_Id(Long productId);
}