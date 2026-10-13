package com.compus.campusmarket.domain.product.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// 조회수 증가(getProductDetail)가 좋아요 토글(toggleLike)이 쓰는 Product.version을 건드리지 않는지 검증하고(회귀 방지),
// 같은 상품에 조회와 좋아요가 섞였을 때 좋아요 재시도/실패와 조회 실패가 얼마나 나오는지 측정하기 위한 테스트.
@Slf4j
@SpringBootTest
class ProductViewLikeConcurrencyTest {

    private static final String RETRY_LOG_PREFIX = "좋아요 처리 충돌 발생";

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductLikeRepository productLikeRepository;
    @Autowired
    private UserRepository userRepository;

    private final List<Long> createdUserIds = new ArrayList<>();
    private Long productId;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(newUser("seller"));
        createdUserIds.add(seller.getId());
        productId = productRepository.save(
                Product.create("조회+좋아요 혼합 테스트 상품", "설명", 10_000L, seller, "전자기기")).getId();
    }

    @AfterEach
    void tearDown() {
        productLikeRepository.deleteAllByProduct_Id(productId); // FK 제약 때문에 상품보다 먼저 삭제
        productRepository.deleteById(productId);
        userRepository.deleteAllById(createdUserIds);
    }

    @Test
    @DisplayName("조회수 증가(getProductDetail)는 viewCount만 +1 하고 Product.version은 올리지 않아야 한다")
    void viewCountIncrease_doesNotBumpProductVersion() {
        Product before = productRepository.findById(productId).orElseThrow();

        LogCollector sqlLog = LogCollector.attach("org.hibernate.SQL", Level.DEBUG);
        try {
            productService.getProductDetail(productId, null);
        } finally {
            sqlLog.detach();
        }

        Product after = productRepository.findById(productId).orElseThrow();
        List<String> productUpdates = sqlLog.messages().stream()
                .filter(sql -> sql.trim().startsWith("update product "))
                .toList();

        productUpdates.forEach(sql -> log.info("[조회수 SQL] {}", sql.trim()));
        log.info("[조회수-버전] 조회 전 version={}, viewCount={} / 조회 후 version={}, viewCount={}",
                before.getVersion(), before.getViewCount(), after.getVersion(), after.getViewCount());

        assertThat(productUpdates).hasSize(1);
        assertThat(productUpdates.get(0)).doesNotContain("version");
        assertThat(after.getViewCount()).isEqualTo(before.getViewCount() + 1);
        assertThat(after.getVersion()).isEqualTo(before.getVersion());
    }

    @Test
    @DisplayName("판매자 본인의 조회는 조회수를 올리지 않고, 다른 로그인 유저의 조회는 올린다")
    void viewCountIncrease_skipsSellerOwnView() {
        User viewer = userRepository.save(newUser("viewer"));
        createdUserIds.add(viewer.getId());
        Long sellerId = createdUserIds.get(0);

        assertThat(productService.getProductDetail(productId, sellerId).getViewCount()).isEqualTo(0);
        // 응답에도 방금 올린 조회수가 반영돼야 한다.
        assertThat(productService.getProductDetail(productId, viewer.getId()).getViewCount()).isEqualTo(1);

        assertThat(productRepository.findById(productId).orElseThrow().getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("[측정-기준선] 좋아요 20건만 동시에 요청")
    void likeOnly_baseline() throws InterruptedException {
        measure(20, 0, 0);
    }

    @Test
    @DisplayName("[측정] 좋아요 20건 + 조회 20건(20스레드 x 1회)을 동시에 요청")
    void likeMixedWithViews_20() throws InterruptedException {
        measure(20, 20, 1);
    }

    @Test
    @DisplayName("[측정] 좋아요 20건 + 조회 100건(20스레드 x 5회)을 동시에 요청")
    void likeMixedWithViews_100() throws InterruptedException {
        measure(20, 20, 5);
    }

    @Test
    @DisplayName("[측정] 조회만 20건(20스레드 x 1회)을 동시에 요청")
    void viewOnly_20() throws InterruptedException {
        measure(0, 20, 1);
    }

    @Test
    @DisplayName("[측정] 조회만 100건(20스레드 x 5회)을 동시에 요청")
    void viewOnly_100() throws InterruptedException {
        measure(0, 20, 5);
    }

    private void measure(int likeRequests, int viewerThreads, int viewsPerThread) throws InterruptedException {
        List<Long> likerIds = new ArrayList<>();
        for (int i = 0; i < likeRequests; i++) {
            User liker = userRepository.save(newUser("mix" + i));
            likerIds.add(liker.getId());
            createdUserIds.add(liker.getId());
        }

        AtomicInteger likeSuccess = new AtomicInteger();
        AtomicInteger viewSuccess = new AtomicInteger();
        Map<String, AtomicInteger> likeFailures = new ConcurrentHashMap<>();
        Map<String, AtomicInteger> viewFailures = new ConcurrentHashMap<>();

        int threads = likeRequests + viewerThreads;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        // 재시도 횟수는 운영 코드를 건드리지 않고 ProductService.toggleLike가 충돌 시 남기는 WARN 로그로 센다.
        LogCollector retryLog = LogCollector.attach(ProductService.class.getName(), Level.WARN);
        try {
            for (Long likerId : likerIds) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        productService.toggleLike(productId, likerId);
                        likeSuccess.incrementAndGet();
                    } catch (Exception e) {
                        count(likeFailures, e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            for (int t = 0; t < viewerThreads; t++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        for (int i = 0; i < viewsPerThread; i++) {
                            try {
                                productService.getProductDetail(productId, null);
                                viewSuccess.incrementAndGet();
                            } catch (Exception e) {
                                count(viewFailures, e);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await();
            start.countDown();
            boolean finishedInTime = done.await(30, TimeUnit.SECONDS);
            executor.shutdownNow();
            assertThat(finishedInTime).isTrue();
        } finally {
            retryLog.detach();
        }

        // 충돌 로그의 마지막 인자가 예외 클래스명(원인)이다.
        Map<String, Integer> retriesByCause = new TreeMap<>();
        for (ILoggingEvent event : retryLog.events()) {
            if (event.getMessage().startsWith(RETRY_LOG_PREFIX)) {
                Object[] args = event.getArgumentArray();
                retriesByCause.merge(String.valueOf(args[args.length - 1]), 1, Integer::sum);
            }
        }
        int conflicts = retriesByCause.values().stream().mapToInt(Integer::intValue).sum();
        int likeFailed = total(likeFailures);
        int viewFailed = total(viewFailures);

        Product reloaded = productRepository.findById(productId).orElseThrow();
        long actualLikeRows = productLikeRepository.countByProduct_Id(productId);

        log.info("[혼합 측정] 좋아요 요청={}, 조회 요청={}({}스레드 x {}회)",
                likeRequests, viewerThreads * viewsPerThread, viewerThreads, viewsPerThread);
        log.info("[혼합 측정] 좋아요 충돌 로그 수={} (원인별 {}), 그중 재시도로 이어진 횟수={}",
                conflicts, retriesByCause, conflicts - likeFailed);
        log.info("[혼합 측정] 좋아요 성공={}, 좋아요 실패={} {}", likeSuccess.get(), likeFailed, likeFailures);
        log.info("[혼합 측정] 조회 성공={}, 조회 실패={} {}", viewSuccess.get(), viewFailed, viewFailures);
        log.info("[혼합 측정] 최종 likeCount={}, 실제 like row 수={}, viewCount={}, version={}",
                reloaded.getLikeCount(), actualLikeRows, reloaded.getViewCount(), reloaded.getVersion());

        // 측정이 목적이라 충돌/실패 건수는 단언하지 않고, 어떤 경우에도 지켜져야 하는 정합성만 검증한다.
        assertThat(reloaded.getLikeCount()).isEqualTo(actualLikeRows);
        assertThat(actualLikeRows).isEqualTo(likeSuccess.get());
        assertThat(reloaded.getViewCount()).isEqualTo(viewSuccess.get());
    }

    private static void count(Map<String, AtomicInteger> failures, Exception e) {
        failures.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
    }

    private static int total(Map<String, AtomicInteger> failures) {
        return failures.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    private User newUser(String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password");
    }

    // 특정 로거의 이벤트를 테스트 구간 동안만 모으고, 끝나면 로그 레벨을 원래대로 되돌린다.
    private static class LogCollector extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
        private final Logger logger;
        private final Level originalLevel;

        private LogCollector(Logger logger) {
            this.logger = logger;
            this.originalLevel = logger.getLevel();
        }

        static LogCollector attach(String loggerName, Level level) {
            Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
            LogCollector collector = new LogCollector(logger);
            collector.setContext(logger.getLoggerContext());
            collector.start();
            logger.setLevel(level);
            logger.addAppender(collector);
            return collector;
        }

        void detach() {
            logger.detachAppender(this);
            logger.setLevel(originalLevel);
            stop();
        }

        List<ILoggingEvent> events() {
            return events;
        }

        List<String> messages() {
            return events.stream().map(ILoggingEvent::getFormattedMessage).toList();
        }

        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
        }
    }
}
