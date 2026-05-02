package com.compus.campusmarket.domain.product.review.service;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.product.review.dto.ReviewRequest;
import com.compus.campusmarket.domain.product.review.entity.Review;
import com.compus.campusmarket.domain.product.review.repository.ReviewRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public void writeReview(Long productId, Long writerId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // 1. 거래 완료 상태인지 확인
        if (product.getStatus() != ProductStatus.SOLD_OUT) {
            throw new IllegalStateException("거래가 완료된 상품만 리뷰를 작성할 수 있습니다.");
        }

        // 2. 작성자가 구매자인지 확인
        if (!product.getBuyer().getId().equals(writerId)) {
            throw new IllegalStateException("구매자만 리뷰를 작성할 수 있습니다.");
        }

        User writer = product.getBuyer();
        User target = product.getSeller();

        // 3. 리뷰 저장
        Review review = Review.create(product, writer, target, request.getRating(), request.getContent());
        reviewRepository.save(review);

        // 4. 판매자의 신뢰 지수(매너 온도) 업데이트
        updateUserTrustScore(target);
    }

    private void updateUserTrustScore(User user) {
        // 해당 유저가 받은 모든 별점의 평균 계산
        List<Review> reviews = reviewRepository.findByTarget(user);
        double average = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        user.updateTrustScore(average);
    }
}