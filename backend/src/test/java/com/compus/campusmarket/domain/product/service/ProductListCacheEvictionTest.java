package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.ai.service.AiService;
import com.compus.campusmarket.domain.product.dto.ProductCreateRequest;
import com.compus.campusmarket.domain.product.dto.ProductListResponse;
import com.compus.campusmarket.domain.product.dto.ProductUpdateRequest;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

// 홈 화면은 GET /api/products/search 첫 페이지를 부르고, 첫 페이지는 Redis 캐시(products-v2, TTL 10분)를 거친다.
// 상품을 바꾸는 쓰기 작업 뒤에 같은 조건으로 다시 조회했을 때 바뀐 내용이 바로 보이는지 검증한다.
// 테스트마다 고유 키워드를 써서 캐시 키가 다른 테스트·개발 데이터와 겹치지 않게 한다.
@SpringBootTest
class ProductListCacheEvictionTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 등록 시 Gemini 검수를 실제로 호출하지 않도록 막는다(외부 API·네트워크 상태에 테스트가 흔들리지 않게).
    @MockitoBean
    private AiService aiService;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private String keyword;
    private User seller;

    @BeforeEach
    void setUp() {
        given(aiService.isAppropriateProduct(anyString())).willReturn(true);
        keyword = "캐시무효화-" + UUID.randomUUID();
        seller = saveUser("seller");
    }

    @AfterEach
    void tearDown() {
        createdProductIds.stream()
                .filter(productRepository::existsById)
                .forEach(productRepository::deleteById);
        userRepository.deleteAllById(createdUserIds);
    }

    @Test
    void 상품을_등록하면_첫_페이지_목록에_바로_보인다() {
        Long existingId = createProduct(keyword + " 기존 상품");
        assertThat(searchIds()).containsExactly(existingId); // 첫 조회로 캐시에 저장

        Long newId = createProduct(keyword + " 새 상품");

        assertThat(searchIds()).contains(newId);
    }

    @Test
    void 판매완료로_바꾸면_기본_목록에서_빠진다() {
        Long productId = createProduct(keyword + " 상품");
        assertThat(searchIds()).containsExactly(productId);

        productService.updateStatus(productId, seller.getId(), ProductStatus.SOLD_OUT);

        // 기본 목록(status 미지정)은 판매완료를 제외한다. 캐시가 남아 있으면 SELLING 상태로 계속 보인다.
        assertThat(search()).noneMatch(p -> p.getId().equals(productId));
    }

    @Test
    void 삭제한_상품은_목록에서_사라진다() {
        Long productId = createProduct(keyword + " 상품");
        assertThat(searchIds()).containsExactly(productId);

        productService.deleteProduct(productId, seller.getId());

        assertThat(searchIds()).isEmpty();
    }

    @Test
    void 상품을_수정하면_목록에_바뀐_가격이_보인다() {
        Long productId = createProduct(keyword + " 상품");
        assertThat(search().get(0).getPrice()).isEqualTo(10_000L);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle(keyword + " 상품");
        request.setDescription("설명");
        request.setPrice(5_000L);
        request.setCategory("전자기기");
        request.setRemainingImageUrls(Collections.emptyList());
        productService.updateProduct(productId, seller.getId(), request, Collections.emptyList());

        assertThat(search().get(0).getPrice()).isEqualTo(5_000L);
    }

    @Test
    void 거래를_완료하면_기본_목록에서_빠진다() {
        User buyer = saveUser("buyer");
        Long productId = createProduct(keyword + " 상품");
        assertThat(searchIds()).containsExactly(productId);

        productService.completeTrade(productId, seller.getId(), buyer.getId());

        assertThat(searchIds()).doesNotContain(productId);
    }

    // 쓰기 메서드가 더 큰 트랜잭션 안에서 호출되면, 메서드가 반환될 때(=바깥 트랜잭션 커밋 전) 캐시가 비워진다.
    // 그 사이에 다른 요청이 조회하면 아직 커밋 전이라 DB에서 옛 값을 읽고, 그 옛 값을 다시 캐시에 넣는다.
    // 커밋 이후에 목록이 최신이어야 하고, 커밋 직후 Redis에 이 조건의 캐시 키가 남아 있지 않아야 한다.
    @Test
    void 바깥_트랜잭션_커밋_전에_들어온_조회가_옛_목록을_캐시에_다시_넣지_못한다() {
        Long productId = createProduct(keyword + " 상품");
        assertThat(searchIds()).containsExactly(productId); // 캐시에 [판매중] 저장

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            productService.updateStatus(productId, seller.getId(), ProductStatus.SOLD_OUT);

            // 커밋 전, 다른 스레드(=다른 사용자의 요청)가 같은 조건으로 조회한다.
            // 별도 커넥션이라 아직 커밋되지 않은 SOLD_OUT은 보이지 않는다.
            List<Long> seenBeforeCommit = CompletableFuture.supplyAsync(this::searchIds).join();
            assertThat(seenBeforeCommit).containsExactly(productId);
        });

        // 커밋 직후, 조회하기 전에 Redis에 이 키워드의 캐시 키가 남아 있지 않아야 한다(커밋 후 clear가 바로 반영됨).
        assertThat(redisTemplate.keys(ProductCacheService.PRODUCTS_CACHE + "::" + keyword + "*")).isEmpty();
        assertThat(searchIds()).doesNotContain(productId);
    }

    private List<ProductListResponse> search() {
        return productService.search(keyword, null, null, null, FIRST_PAGE);
    }

    private List<Long> searchIds() {
        return search().stream().map(ProductListResponse::getId).toList();
    }

    private Long createProduct(String title) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle(title);
        request.setDescription("설명");
        request.setPrice(10_000L);
        request.setCategory("전자기기");
        Long id = productService.createProduct(seller.getId(), request, Collections.emptyList());
        createdProductIds.add(id);
        return id;
    }

    private User saveUser(String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(
                User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password"));
        createdUserIds.add(user.getId());
        return user;
    }
}
