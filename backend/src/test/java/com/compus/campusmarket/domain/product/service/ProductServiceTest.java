package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.chat.entity.ChatMessage;
import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import com.compus.campusmarket.domain.chat.repository.ChatMessageRepository;
import com.compus.campusmarket.domain.chat.repository.ChatRoomRepository;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductLike;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.product.review.entity.Review;
import com.compus.campusmarket.domain.product.review.repository.ReviewRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

// 찜/채팅방/리뷰가 걸려 있는 상품을 삭제하면 FK 제약(DataIntegrityViolationException)으로
// 실패하던 문제(ProductService.deleteProduct)를 재현하고, 연관 데이터 정리 후 정상 삭제되는지 검증한다.
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductLikeRepository productLikeRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        userRepository.deleteAllById(createdUserIds);
        createdUserIds.clear();
    }

    @Test
    void 찜_채팅방_리뷰가_있는_상품도_삭제할_수_있다() {
        User seller = userRepository.save(newUser("seller"));
        User buyer = userRepository.save(newUser("buyer"));
        createdUserIds.add(seller.getId());
        createdUserIds.add(buyer.getId());

        Product product = productRepository.save(
                Product.create("삭제 대상 상품", "설명", 10_000L, seller, "전자기기"));

        productLikeRepository.save(new ProductLike(buyer, product));

        ChatRoom room = chatRoomRepository.save(
                ChatRoom.builder().product(product).seller(seller).buyer(buyer).build());
        chatMessageRepository.save(
                ChatMessage.builder().chatRoom(room).senderId(buyer.getId()).message("안녕하세요").build());

        reviewRepository.save(Review.create(product, buyer, seller, 5.0, "좋은 거래였습니다"));

        assertThatCode(() -> productService.deleteProduct(product.getId(), seller.getId()))
                .doesNotThrowAnyException();
    }

    private User newUser(String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password");
    }
}
