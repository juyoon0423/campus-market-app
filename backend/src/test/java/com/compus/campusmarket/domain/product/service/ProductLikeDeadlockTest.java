package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 좋아요 INSERT의 FK 검사가 상품 행에 공유 락(S)을 걸고, 이어지는 like_count UPDATE가 같은 행에 배타 락(X)을
// 기다리면서 동시 좋아요끼리 데드락이 났다(docs/like-deadlock/). 같은 상품에 동시에 좋아요가 몰려도
// 재시도 없이(=데드락·버전 충돌 없이) 한 번에 처리되고, 개수가 실제 행 수와 맞아야 한다.
// 재시도 여부는 실제 DB 작업을 하는 ProductLikeService.applyToggle 호출 횟수로 센다(DB 권한 없이도 확인 가능).
@SpringBootTest
class ProductLikeDeadlockTest {

    private static final int CONCURRENCY = 20;

    @Autowired
    private ProductService productService;
    @MockitoSpyBean
    private ProductLikeService productLikeService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductLikeRepository productLikeRepository;
    @Autowired
    private UserRepository userRepository;

    private final List<Long> createdUserIds = new ArrayList<>();
    private Long productId;

    @AfterEach
    void tearDown() {
        if (productId != null) {
            productLikeRepository.deleteAllByProduct_Id(productId);
            productRepository.deleteById(productId);
        }
        userRepository.deleteAllById(createdUserIds);
    }

    @Test
    void 같은_상품에_동시에_좋아요가_몰려도_재시도_없이_모두_반영된다() throws InterruptedException {
        User seller = saveUser("seller");
        productId = productRepository.save(Product.create("데드락 회귀 상품", "설명", 10_000L, seller, "전자기기")).getId();
        List<Long> likerIds = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            likerIds.add(saveUser("liker").getId());
        }
        Mockito.clearInvocations(productLikeService);

        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        runConcurrently(likerIds, likerId -> {
            try {
                productService.toggleLike(productId, likerId);
            } catch (Throwable t) {
                failures.add(t);
            }
        });

        long applyCalls = applyToggleCalls();
        Product reloaded = productRepository.findById(productId).orElseThrow();
        long rows = productLikeRepository.countByProduct_Id(productId);

        assertThat(failures).as("재시도를 모두 소진해 실패한 요청").isEmpty();
        assertThat(applyCalls).as("applyToggle 호출 수(요청 %d건, 초과분 = 재시도)", CONCURRENCY).isEqualTo(CONCURRENCY);
        assertThat(rows).isEqualTo(CONCURRENCY);
        assertThat(reloaded.getLikeCount()).isEqualTo(CONCURRENCY);
    }

    // 이미 좋아요한 10명의 취소와 새로운 10명의 좋아요가 동시에 들어와도 재시도 없이 처리되고,
    // 최종적으로 새로 누른 10명만 남아야 한다.
    @Test
    void 좋아요와_취소가_동시에_섞여도_재시도_없이_최종_상태가_맞다() throws InterruptedException {
        User seller = saveUser("seller");
        productId = productRepository.save(Product.create("데드락 회귀 상품", "설명", 10_000L, seller, "전자기기")).getId();
        List<Long> cancelerIds = new ArrayList<>();
        List<Long> newLikerIds = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY / 2; i++) {
            Long canceler = saveUser("canceler").getId();
            productService.toggleLike(productId, canceler); // 미리 좋아요(순차)
            cancelerIds.add(canceler);
            newLikerIds.add(saveUser("liker").getId());
        }
        List<Long> everyone = new ArrayList<>(cancelerIds);
        everyone.addAll(newLikerIds);
        Mockito.clearInvocations(productLikeService);

        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        runConcurrently(everyone, userId -> {
            try {
                productService.toggleLike(productId, userId);
            } catch (Throwable t) {
                failures.add(t);
            }
        });

        assertThat(failures).isEmpty();
        assertThat(applyToggleCalls()).isEqualTo(everyone.size());
        for (Long canceler : cancelerIds) {
            assertThat(productLikeRepository.existsByUser_IdAndProduct_Id(canceler, productId)).isFalse();
        }
        for (Long liker : newLikerIds) {
            assertThat(productLikeRepository.existsByUser_IdAndProduct_Id(liker, productId)).isTrue();
        }
        assertThat(productLikeRepository.countByProduct_Id(productId)).isEqualTo(newLikerIds.size());
        assertThat(productRepository.findById(productId).orElseThrow().getLikeCount()).isEqualTo(newLikerIds.size());
    }

    // 같은 사용자가 좋아요를 5번 동시에 누르면(연속 클릭) 요청이 한 줄로 처리되어
    // 좋아요 → 취소 → 좋아요 → 취소 → 좋아요가 되고, 유니크 제약 위반이나 재시도 없이 최종 1건이 남아야 한다.
    @Test
    void 같은_사용자가_연속으로_눌러도_순서대로_토글되고_재시도가_없다() throws InterruptedException {
        int clicks = 5;
        User seller = saveUser("seller");
        productId = productRepository.save(Product.create("데드락 회귀 상품", "설명", 10_000L, seller, "전자기기")).getId();
        Long userId = saveUser("clicker").getId();
        Mockito.clearInvocations(productLikeService);

        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        runConcurrently(java.util.Collections.nCopies(clicks, userId), id -> {
            try {
                results.add(productService.toggleLike(productId, id));
            } catch (Throwable t) {
                failures.add(t);
            }
        });

        assertThat(failures).isEmpty();
        assertThat(applyToggleCalls()).isEqualTo(clicks);
        assertThat(results).filteredOn(r -> r.contains("추가")).hasSize(3);
        assertThat(results).filteredOn(r -> r.contains("취소")).hasSize(2);
        assertThat(productLikeRepository.countByProduct_Id(productId)).isEqualTo(1);
        assertThat(productRepository.findById(productId).orElseThrow().getLikeCount()).isEqualTo(1);
    }

    private long applyToggleCalls() {
        return Mockito.mockingDetails(productLikeService).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("applyToggle"))
                .count();
    }

    private User saveUser(String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(
                User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password"));
        createdUserIds.add(user.getId());
        return user;
    }

    private void runConcurrently(List<Long> inputs, java.util.function.Consumer<Long> action) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(inputs.size());
        CountDownLatch ready = new CountDownLatch(inputs.size());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(inputs.size());
        for (Long input : inputs) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    action.accept(input);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).as("30초 안에 모든 요청이 끝나야 한다").isTrue();
        executor.shutdownNow();
    }
}
