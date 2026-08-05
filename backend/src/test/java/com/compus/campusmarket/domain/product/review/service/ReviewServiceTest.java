package com.compus.campusmarket.domain.product.review.service;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.product.review.dto.ReviewRequest;
import com.compus.campusmarket.domain.product.review.repository.ReviewRepository;
import com.compus.campusmarket.domain.product.service.ProductService;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// existsByProductIdAndWriterId가 정의만 돼 있고 writeReview 호출부에서 안 쓰여서
// 같은 거래에 리뷰를 여러 번 작성할 수 있던 문제(SECURITY_REFACTOR_TODO 🟡 4번)를 검증한다.
@SpringBootTest
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdProductIds.forEach(id -> reviewRepository.deleteAllByProduct_Id(id));
        createdProductIds.forEach(productRepository::deleteById);
        createdProductIds.clear();
        userRepository.deleteAllById(createdUserIds);
        createdUserIds.clear();
    }

    @Test
    void 같은_거래에_리뷰를_두_번_작성하면_두_번째_시도는_거부된다() {
        Long soldOutProductId = createSoldOutProduct();

        assertThatCode(() -> reviewService.writeReview(soldOutProductId, buyerId, reviewRequest(5.0, "좋은 거래였습니다")))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> reviewService.writeReview(soldOutProductId, buyerId, reviewRequest(4.0, "한 번 더 씁니다")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 리뷰를 작성한 거래입니다.");

        assertThat(reviewRepository.existsByProductIdAndWriterId(soldOutProductId, buyerId)).isTrue();
    }

    private Long buyerId;

    private Long createSoldOutProduct() {
        User seller = userRepository.save(newUser("seller"));
        User buyer = userRepository.save(newUser("buyer"));
        createdUserIds.add(seller.getId());
        createdUserIds.add(buyer.getId());
        buyerId = buyer.getId();

        Product product = productRepository.save(
                Product.create("리뷰 테스트 상품", "설명", 10_000L, seller, "전자기기"));
        createdProductIds.add(product.getId());

        productService.completeTrade(product.getId(), seller.getId(), buyer.getId());

        return product.getId();
    }

    private ReviewRequest reviewRequest(double rating, String content) {
        ReviewRequest request = new ReviewRequest();
        ReflectionTestUtils.setField(request, "rating", rating);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private User newUser(String tag) {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        return User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password");
    }
}
