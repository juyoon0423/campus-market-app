package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.entity.ProductStatus;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class ProductConcurrencyTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductLikeService productLikeService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductLikeRepository productLikeRepository;
    @Autowired
    private UserRepository userRepository;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private User seller;
    private Long sellerId;
    private Long productId;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(newUser("seller"));
        sellerId = seller.getId();
        createdUserIds.add(sellerId);

        productId = createProduct(seller);
    }

    @AfterEach
    void tearDown() {
        for (Long id : createdProductIds) {
            productLikeRepository.deleteAllByProduct_Id(id); // FK 제약 때문에 상품보다 먼저 삭제
            productRepository.deleteById(id);
        }
        userRepository.deleteAllById(createdUserIds);
    }

    private Long createProduct(User seller) {
        Product product = productRepository.save(
                Product.create("동시성 테스트 상품", "설명", 10_000L, seller, "전자기기"));
        createdProductIds.add(product.getId());
        return product.getId();
    }

    @Test
    @DisplayName("서로 다른 유저 N명이 동시에 좋아요를 눌러도 likeCount와 실제 저장된 row 수가 일치해야 한다")
    void concurrentLike_countShouldMatchActualRows() throws InterruptedException {
        int concurrency = 20; // 프로젝트 기존 k6 부하테스트(VU 20)와 동일한 동시성 수준
        List<Long> likerIds = createLikers(concurrency, "liker");

        runConcurrently(concurrency, likerIds, likerId -> productService.toggleLike(productId, likerId));

        Product reloaded = productRepository.findById(productId).orElseThrow();
        long actualLikeRows = productLikeRepository.countByProduct_Id(productId);

        log.info("[좋아요 동시성] 요청 수={}, product.likeCount={}, 실제 저장된 row 수={}",
                concurrency, reloaded.getLikeCount(), actualLikeRows);

        assertThat(reloaded.getLikeCount()).isEqualTo(actualLikeRows);
        assertThat(actualLikeRows).isEqualTo(concurrency);
    }

    @Test
    @DisplayName("서로 다른 구매자 2명이 동시에 거래완료를 시도하면 정확히 한 명만 성공하고, 최종 buyer가 그 성공자와 일치해야 한다")
    void concurrentCompleteTrade_onlyOneWinnerShouldPersist() throws InterruptedException {
        User buyerA = userRepository.save(newUser("buyerA"));
        User buyerB = userRepository.save(newUser("buyerB"));
        createdUserIds.add(buyerA.getId());
        createdUserIds.add(buyerB.getId());

        List<Long> buyerIds = List.of(buyerA.getId(), buyerB.getId());
        AtomicInteger successCount = new AtomicInteger();
        AtomicReference<Long> winnerBuyerId = new AtomicReference<>();

        int concurrency = buyerIds.size();
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(concurrency);

        for (Long buyerId : buyerIds) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    productService.completeTrade(productId, sellerId, buyerId);
                    successCount.incrementAndGet();
                    winnerBuyerId.set(buyerId);
                } catch (Exception e) {
                    log.warn("[거래완료 동시성] buyerId={} 실패: {}", buyerId, e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await(20, TimeUnit.SECONDS);
        executor.shutdownNow(); // 타임아웃 후에도 재시도 중인 스레드가 백그라운드에 남지 않도록 강제 종료

        Product reloaded = productRepository.findById(productId).orElseThrow();
        log.info("[거래완료 동시성] 성공 횟수={}, 최종 status={}, 최종 buyerId={}, 승자로 기록된 buyerId={}",
                successCount.get(), reloaded.getStatus(),
                reloaded.getBuyer() != null ? reloaded.getBuyer().getId() : null, winnerBuyerId.get());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(reloaded.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(reloaded.getBuyer()).isNotNull();
        assertThat(reloaded.getBuyer().getId()).isEqualTo(winnerBuyerId.get());
    }

    @Test
    @DisplayName("좋아요 낙관적 락+재시도 vs 비관적 락의 처리량을 실측 비교한다")
    void benchmarkOptimisticVsPessimisticLike() throws InterruptedException {
        int concurrency = 20; // 프로젝트 기존 k6 부하테스트(VU 20)와 동일한 동시성 수준

        Long optimisticProductId = productId; // setUp에서 만든 상품 재사용
        List<Long> optimisticLikerIds = createLikers(concurrency, "opt");
        long optimisticElapsedMs = measureElapsedMs(() ->
                runConcurrently(concurrency, optimisticLikerIds,
                        likerId -> productService.toggleLike(optimisticProductId, likerId)));
        long optimisticActualRows = productLikeRepository.countByProduct_Id(optimisticProductId);

        Long pessimisticProductId = createProduct(seller);
        List<Long> pessimisticLikerIds = createLikers(concurrency, "pes");
        long pessimisticElapsedMs = measureElapsedMs(() ->
                runConcurrently(concurrency, pessimisticLikerIds,
                        likerId -> productLikeService.applyTogglePessimistic(pessimisticProductId, likerId)));
        long pessimisticActualRows = productLikeRepository.countByProduct_Id(pessimisticProductId);

        log.info("[벤치마크] 동시 요청 수={}", concurrency);
        log.info("[벤치마크-낙관적+재시도] 소요={}ms, 실제 저장된 row 수={}", optimisticElapsedMs, optimisticActualRows);
        log.info("[벤치마크-비관적] 소요={}ms, 실제 저장된 row 수={}", pessimisticElapsedMs, pessimisticActualRows);

        assertThat(optimisticActualRows).isEqualTo(concurrency);
        assertThat(pessimisticActualRows).isEqualTo(concurrency);
    }

    private List<Long> createLikers(int count, String tagPrefix) {
        List<Long> likerIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User liker = userRepository.save(newUser(tagPrefix + i));
            likerIds.add(liker.getId());
            createdUserIds.add(liker.getId());
        }
        return likerIds;
    }

    private long measureElapsedMs(ThrowingRunnable action) throws InterruptedException {
        long start = System.currentTimeMillis();
        action.run();
        return System.currentTimeMillis() - start;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws InterruptedException;
    }

    private <T> void runConcurrently(int concurrency, List<T> inputs, ThrowingConsumer<T> action) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(concurrency);

        for (T input : inputs) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    action.accept(input);
                } catch (Exception e) {
                    log.warn("동시 요청 실패: {}", e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        boolean finishedInTime = done.await(20, TimeUnit.SECONDS);
        executor.shutdownNow(); // 타임아웃 후에도 재시도 중인 스레드가 백그라운드에 남지 않도록 강제 종료
        if (!finishedInTime) {
            log.warn("동시 요청이 제한 시간 내에 모두 끝나지 않았습니다. 이후 검증 결과가 실제와 다를 수 있습니다.");
        }
    }

    private User newUser(String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password");
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T input) throws Exception;
    }
}
