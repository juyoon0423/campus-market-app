package com.compus.campusmarket.domain.product.review.controller;

import com.compus.campusmarket.domain.product.review.dto.ReviewRequest;
import com.compus.campusmarket.domain.product.review.service.ReviewService;
import com.compus.campusmarket.global.config.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{productId}")
    public ResponseEntity<String> writeReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 서비스 로직 호출: 상품ID, 작성자ID, 리뷰 데이터를 전달
        reviewService.writeReview(productId, userDetails.getUserId(), request);

        return ResponseEntity.ok("리뷰 작성이 완료되었습니다.");
    }
}