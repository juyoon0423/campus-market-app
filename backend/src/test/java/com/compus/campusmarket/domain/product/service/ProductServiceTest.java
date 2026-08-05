package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.chat.entity.ChatMessage;
import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import com.compus.campusmarket.domain.chat.repository.ChatMessageRepository;
import com.compus.campusmarket.domain.chat.repository.ChatRoomRepository;
import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductDetailResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductLike;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
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
    private final List<Long> createdProductIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdProductIds.forEach(productRepository::deleteById);
        createdProductIds.clear();
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

    @Test
    void 상품_등록시_거래_희망_장소가_저장된다() {
        User seller = userRepository.save(newUser("seller"));
        createdUserIds.add(seller.getId());

        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle("상품");
        request.setDescription("설명");
        request.setPrice(10_000L);
        request.setCategory("전자기기");
        request.setTradeLocationName("학생회관 앞");
        request.setTradeLatitude(37.360);
        request.setTradeLongitude(127.972);

        Long productId = productService.createProduct(seller.getId(), request, Collections.emptyList());
        createdProductIds.add(productId);

        ProductDetailResponse detail = productService.getProductDetail(productId, seller.getId());

        assertThat(detail.getTradeLocationName()).isEqualTo("학생회관 앞");
        assertThat(detail.getTradeLatitude()).isEqualTo(37.360);
        assertThat(detail.getTradeLongitude()).isEqualTo(127.972);
    }

    @Test
    void 상품_수정시_거래_희망_장소를_변경할_수_있다() {
        User seller = userRepository.save(newUser("seller"));
        createdUserIds.add(seller.getId());

        ProductCreateRequest createRequest = new ProductCreateRequest();
        createRequest.setTitle("상품");
        createRequest.setDescription("설명");
        createRequest.setPrice(10_000L);
        createRequest.setCategory("전자기기");
        createRequest.setTradeLocationName("옛 장소");
        createRequest.setTradeLatitude(37.0);
        createRequest.setTradeLongitude(127.0);

        Long productId = productService.createProduct(seller.getId(), createRequest, Collections.emptyList());
        createdProductIds.add(productId);

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setTitle("상품");
        updateRequest.setDescription("설명");
        updateRequest.setPrice(10_000L);
        updateRequest.setCategory("전자기기");
        updateRequest.setTradeLocationName("새 장소");
        updateRequest.setTradeLatitude(37.5);
        updateRequest.setTradeLongitude(127.5);
        updateRequest.setRemainingImageUrls(Collections.emptyList());

        productService.updateProduct(productId, seller.getId(), updateRequest, Collections.emptyList());

        ProductDetailResponse detail = productService.getProductDetail(productId, seller.getId());
        assertThat(detail.getTradeLocationName()).isEqualTo("새 장소");
        assertThat(detail.getTradeLatitude()).isEqualTo(37.5);
        assertThat(detail.getTradeLongitude()).isEqualTo(127.5);
    }

    @Test
    void 검색시_상태를_지정하지_않으면_판매완료_상품은_제외된다() {
        User seller = userRepository.save(newUser("seller"));
        createdUserIds.add(seller.getId());
        String keyword = "검색키워드-" + UUID.randomUUID();

        Long sellingId = createProduct(seller.getId(), keyword + " 판매중 상품");
        Long soldOutId = createProduct(seller.getId(), keyword + " 판매완료 상품");
        createdProductIds.add(sellingId);
        createdProductIds.add(soldOutId);
        productService.updateStatus(soldOutId, seller.getId(), ProductStatus.SOLD_OUT);

        Pageable pageable = PageRequest.of(0, 20);

        List<Long> defaultResultIds = productService.search(keyword, null, null, null, pageable).stream()
                .map(response -> response.getId())
                .collect(Collectors.toList());
        assertThat(defaultResultIds).containsExactly(sellingId);

        List<Long> soldOutResultIds = productService.search(keyword, null, ProductStatus.SOLD_OUT, null, pageable)
                .stream()
                .map(response -> response.getId())
                .collect(Collectors.toList());
        assertThat(soldOutResultIds).containsExactly(soldOutId);
    }

    private Long createProduct(Long sellerId, String title) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle(title);
        request.setDescription("설명");
        request.setPrice(10_000L);
        request.setCategory("전자기기");
        request.setTradeLocationName("학생회관 앞");
        request.setTradeLatitude(37.360);
        request.setTradeLongitude(127.972);

        return productService.createProduct(sellerId, request, Collections.emptyList());
    }

    private User newUser(String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password");
    }
}
