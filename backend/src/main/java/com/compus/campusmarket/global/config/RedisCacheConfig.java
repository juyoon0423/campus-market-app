package com.compus.campusmarket.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer; // 👈 최신 인터페이스

import java.time.Duration;

@EnableCaching
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 캐시 수명 10분
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        // 상품 쓰기 작업마다 캐시 전체를 비운다(@CacheEvict allEntries).
        // - batchStrategy: 기본값은 KEYS 명령인데, KEYS는 Redis 전체 키를 훑는 동안 다른 명령을 막는다.
        //   SCAN으로 나눠서 지우게 바꾼다.
        // - immediateWrites: Spring Data Redis 4.0은 Lettuce를 쓰면 기본으로 put/clear를 비동기로 보내고
        //   완료를 기다리지 않는다. 그러면 clear()가 반환된 뒤에도 삭제(UNLINK)가 아직 Redis에 도착하지 않아,
        //   바로 다음 조회가 옛 캐시를 읽는다(redis-cli MONITOR로 SCAN → GET → UNLINK 순서를 확인함).
        //   쓰기가 끝날 때까지 기다리게 한다.
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(connectionFactory, writer -> writer
                .batchStrategy(BatchStrategies.scan(1000))
                .immediateWrites());

        // transactionAware: 트랜잭션 안에서 호출된 clear/put을 커밋 직후로 미룬다(트랜잭션 밖이면 즉시 실행).
        // 쓰기 메서드가 더 큰 트랜잭션 안에서 불리면 커밋 전에 캐시가 비워지고, 그 사이 들어온 조회가
        // 커밋 전 옛 값을 다시 캐시에 넣을 수 있다(ProductListCacheEvictionTest의 바깥 트랜잭션 케이스).
        return RedisCacheManager.builder(cacheWriter)
                .transactionAware()
                .cacheDefaults(cacheConfig)
                .build();
    }
}