package com.compus.campusmarket.domain.product.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.repository.ProductLikeRepository;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// 좋아요 경합 수준별 측정. 평소 테스트 스위트에서는 돌지 않고, 환경변수를 줄 때만 실행된다.
//   LIKE_BENCH=true LIKE_BENCH_OUT=<저장 폴더> ./gradlew test --tests '*LikeContentionBenchmarkTest'
// 세 시나리오 모두 라운드마다 서로 다른 사용자 20명이 동시에 toggleLike(좋아요 추가)를 1번씩 호출하고, 50라운드 = 1,000건.
//   high: 상품 1개(상품당 동시 20건) / mid: 상품 5개(상품당 동시 4건) / none: 상품 20개(상품당 1건)
// 데드락 수와 행 락 대기 수는 InnoDB 지표(INNODB_METRICS) 증가분이라 PROCESS 권한이 필요하다(Docker MySQL에서 측정).
// 재시도 원인은 ProductService의 재시도 경고 로그("원인=...")를 세고(비관적 락 전환 후에는 재시도 루프가 없어 0),
// 재시도 소진은 toggleLike가 예외로 끝난 요청 수로 센다.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LIKE_BENCH", matches = "true")
class LikeContentionBenchmarkTest {

    private static final int CONCURRENCY = 20;
    private static final int ROUNDS = 50;
    private static final int WARMUP_ROUNDS = 2; // JIT·커넥션 풀 예열. 기록하지 않는다(수정 전후 동일)

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
    void measure() throws Exception {
        Path outDir = Path.of(System.getenv().getOrDefault("LIKE_BENCH_OUT", "build/like-bench"));
        Files.createDirectories(outDir);
        StringBuilder summary = new StringBuilder(
                "scenario\tproducts\trequests\tdeadlocks\trow_lock_waits\tretry_deadlock\tretry_optimistic\tretry_unique\t"
                        + "exhausted\tcount_mismatch\tlatency_p50_ms\tlatency_p95_ms\tlatency_max_ms\tround_p50_ms\tround_p95_ms\ttotal_s\n");
        for (Object[] scenario : new Object[][]{{"high", 1}, {"mid", 5}, {"none", 20}}) {
            summary.append(runScenario((String) scenario[0], (Integer) scenario[1], outDir));
        }
        Files.writeString(outDir.resolve("summary.tsv"), summary.toString());
        System.out.println("[좋아요 경합 측정]\n" + summary);
    }

    private String runScenario(String name, int productCount, Path outDir) throws Exception {
        Logger serviceLogger = (Logger) LoggerFactory.getLogger(ProductService.class);
        ListAppender<ILoggingEvent> retryLogs = new ListAppender<>();
        retryLogs.start();

        List<Double> latencies = new ArrayList<>();
        List<Double> roundTimes = new ArrayList<>();
        StringBuilder requestsCsv = new StringBuilder("round,product_index,latency_ms,outcome\n");
        StringBuilder roundsTsv = new StringBuilder("round\tround_ms\tdeadlocks\trow_lock_waits\texhausted\tcount_mismatch\n");
        long deadlocks = 0, waits = 0, exhausted = 0, mismatch = 0;
        long started = System.nanoTime();

        for (int round = 1 - WARMUP_ROUNDS; round <= ROUNDS; round++) {
            boolean recorded = round >= 1;
            List<Long> userIds = new ArrayList<>();
            User seller = saveUser("seller", userIds);
            List<Long> productIds = new ArrayList<>();
            for (int p = 0; p < productCount; p++) {
                productIds.add(productRepository.save(
                        Product.create("경합 측정 상품", "설명", 10_000L, seller, "전자기기")).getId());
            }
            List<Long> likerIds = new ArrayList<>();
            for (int i = 0; i < CONCURRENCY; i++) {
                likerIds.add(saveUser("liker", userIds).getId());
            }

            if (recorded) {
                serviceLogger.addAppender(retryLogs);
            }
            long deadlocksBefore = metric("lock_deadlocks");
            long waitsBefore = metric("lock_row_lock_waits");
            double[] latencyMs = new double[CONCURRENCY];
            String[] outcome = new String[CONCURRENCY];
            long roundStart = System.nanoTime();
            runConcurrently(CONCURRENCY, i -> {
                long t0 = System.nanoTime();
                try {
                    productService.toggleLike(productIds.get(i % productCount), likerIds.get(i));
                    outcome[i] = "ok";
                } catch (Exception e) {
                    outcome[i] = e.getClass().getSimpleName();
                } finally {
                    latencyMs[i] = (System.nanoTime() - t0) / 1_000_000.0;
                }
            });
            double roundMs = (System.nanoTime() - roundStart) / 1_000_000.0;
            long roundDeadlocks = metric("lock_deadlocks") - deadlocksBefore;
            long roundWaits = metric("lock_row_lock_waits") - waitsBefore;
            serviceLogger.detachAppender(retryLogs);

            long roundExhausted = 0;
            for (String o : outcome) {
                if (!"ok".equals(o)) {
                    roundExhausted++;
                }
            }
            long roundMismatch = 0;
            for (Long productId : productIds) {
                Product reloaded = productRepository.findById(productId).orElseThrow();
                if (reloaded.getLikeCount() != productLikeRepository.countByProduct_Id(productId)) {
                    roundMismatch++;
                }
            }

            if (recorded) {
                for (int i = 0; i < CONCURRENCY; i++) {
                    latencies.add(latencyMs[i]);
                    requestsCsv.append(round).append(',').append(i % productCount).append(',')
                            .append(String.format(Locale.ROOT, "%.3f", latencyMs[i])).append(',').append(outcome[i]).append('\n');
                }
                roundTimes.add(roundMs);
                deadlocks += roundDeadlocks;
                waits += roundWaits;
                exhausted += roundExhausted;
                mismatch += roundMismatch;
                roundsTsv.append(round).append('\t').append(String.format(Locale.ROOT, "%.3f", roundMs)).append('\t')
                        .append(roundDeadlocks).append('\t').append(roundWaits).append('\t')
                        .append(roundExhausted).append('\t').append(roundMismatch).append('\n');
            }

            for (Long productId : productIds) {
                productLikeRepository.deleteAllByProduct_Id(productId);
                productRepository.deleteById(productId);
            }
            userRepository.deleteAllById(userIds);
        }
        double totalS = (System.nanoTime() - started) / 1_000_000_000.0;

        long retryDeadlock = countRetry(retryLogs, "CannotAcquireLockException");
        long retryOptimistic = countRetry(retryLogs, "ObjectOptimisticLockingFailureException");
        long retryUnique = countRetry(retryLogs, "DataIntegrityViolationException");
        Files.writeString(outDir.resolve(name + "-requests.csv"), requestsCsv.toString());
        Files.writeString(outDir.resolve(name + "-rounds.tsv"), roundsTsv.toString());

        return String.format(Locale.ROOT, "%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\n",
                name, productCount, latencies.size(), deadlocks, waits, retryDeadlock, retryOptimistic, retryUnique,
                exhausted, mismatch, percentile(latencies, 50), percentile(latencies, 95), percentile(latencies, 100),
                percentile(roundTimes, 50), percentile(roundTimes, 95), totalS);
    }

    private long countRetry(ListAppender<ILoggingEvent> logs, String cause) {
        return logs.list.stream().filter(e -> e.getFormattedMessage().contains("원인=" + cause)).count();
    }

    // nearest-rank 방식
    private double percentile(List<Double> values, int p) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, rank - 1)));
    }

    private long metric(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT count FROM information_schema.INNODB_METRICS WHERE name = ?", Long.class, name);
    }

    private User saveUser(String tag, List<Long> ids) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(
                User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password"));
        ids.add(user.getId());
        return user;
    }

    private void runConcurrently(int count, java.util.function.IntConsumer action) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    action.accept(index);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
    }
}
