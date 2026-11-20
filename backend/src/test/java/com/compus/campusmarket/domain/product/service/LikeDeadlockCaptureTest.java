package com.compus.campusmarket.domain.product.service;

import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// 좋아요 동시 요청에서 생기는 InnoDB 데드락의 원문(SHOW ENGINE INNODB STATUS의 LATEST DETECTED DEADLOCK)을
// 라운드마다 수집하는 진단용 테스트. 평소 테스트 스위트에서는 돌지 않고, 환경변수를 줄 때만 실행된다.
//   LIKE_DEADLOCK_CAPTURE=true LIKE_DEADLOCK_OUT=<저장 폴더> ./gradlew test --tests '*LikeDeadlockCaptureTest'
// DB 계정에 PROCESS 권한이 있어야 한다(SHOW ENGINE INNODB STATUS, INNODB_METRICS).
// LATEST DETECTED DEADLOCK에는 마지막 1건만 남으므로, 라운드를 나눠 라운드마다 마지막 1건을 저장한다.
@Slf4j
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LIKE_DEADLOCK_CAPTURE", matches = "true")
class LikeDeadlockCaptureTest {

    private static final int CONCURRENCY = 20; // ProductConcurrencyTest의 좋아요 동시성 시나리오와 같은 수준

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductLikeRepository productLikeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void captureLatestDeadlockPerRound() throws Exception {
        int rounds = Integer.parseInt(System.getenv().getOrDefault("LIKE_DEADLOCK_ROUNDS", "10"));
        Path outDir = Path.of(System.getenv().getOrDefault("LIKE_DEADLOCK_OUT", "build/like-deadlock"));
        Files.createDirectories(outDir);

        StringBuilder summary = new StringBuilder("round\tproduct_id\tdeadlocks(lock_deadlocks delta)\texhausted\tlike_count\trows\n");
        String lastSection = latestDeadlockSection();

        for (int round = 1; round <= rounds; round++) {
            List<Long> userIds = new ArrayList<>();
            User seller = saveUser("seller", userIds);
            Product product = productRepository.save(Product.create("데드락 수집 상품", "설명", 10_000L, seller, "전자기기"));
            List<Long> likerIds = new ArrayList<>();
            for (int i = 0; i < CONCURRENCY; i++) {
                likerIds.add(saveUser("liker", userIds).getId());
            }

            long deadlocksBefore = deadlockCount();
            AtomicInteger exhausted = new AtomicInteger();
            runConcurrently(likerIds, likerId -> {
                try {
                    productService.toggleLike(product.getId(), likerId);
                } catch (IllegalStateException e) {
                    exhausted.incrementAndGet(); // 예외로 끝난 요청(비관적 락 전환 전에는 6회 재시도 소진)
                }
            });
            long deadlocks = deadlockCount() - deadlocksBefore;

            Product reloaded = productRepository.findById(product.getId()).orElseThrow();
            long rows = productLikeRepository.countByProduct_Id(product.getId());
            summary.append(round).append('\t').append(product.getId()).append('\t').append(deadlocks).append('\t')
                    .append(exhausted.get()).append('\t').append(reloaded.getLikeCount()).append('\t').append(rows).append('\n');

            String section = latestDeadlockSection();
            if (section != null && !section.equals(lastSection)) {
                Files.writeString(outDir.resolve(String.format("deadlock-round-%02d.txt", round)),
                        "# round=" + round + " product_id=" + product.getId() + " liker_ids=" + likerIds + "\n"
                                + "# user ids are test accounts created for this round\n\n" + section);
                lastSection = section;
            }

            productLikeRepository.deleteAllByProduct_Id(product.getId());
            productRepository.deleteById(product.getId());
            userRepository.deleteAllById(userIds);
        }
        Files.writeString(outDir.resolve("summary.tsv"), summary.toString());
        log.info("[데드락 수집]\n{}", summary);
    }

    // LATEST DETECTED DEADLOCK 절만 원문 그대로 잘라 낸다(다음 절 제목 직전까지).
    private String latestDeadlockSection() {
        Map<String, Object> row = jdbcTemplate.queryForMap("SHOW ENGINE INNODB STATUS");
        String status = String.valueOf(row.get("Status"));
        int start = status.indexOf("LATEST DETECTED DEADLOCK");
        if (start < 0) {
            return null;
        }
        int sectionStart = status.lastIndexOf("------------------------", start);
        int end = status.indexOf("------------\nTRANSACTIONS\n", start);
        return status.substring(sectionStart < 0 ? start : sectionStart, end < 0 ? status.length() : end);
    }

    private long deadlockCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count FROM information_schema.INNODB_METRICS WHERE name = 'lock_deadlocks'", Long.class);
    }

    private User saveUser(String tag, List<Long> ids) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(
                User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password"));
        ids.add(user.getId());
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
                } catch (Exception e) {
                    log.warn("동시 요청 실패: {}", e.toString());
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();
    }
}
