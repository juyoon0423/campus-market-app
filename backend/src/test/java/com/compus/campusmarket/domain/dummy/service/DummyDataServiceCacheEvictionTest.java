package com.compus.campusmarket.domain.dummy.service;

import com.compus.campusmarket.domain.product.service.ProductCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

// 더미 데이터는 JDBC로 직접 넣어 ProductService의 캐시 비우기를 거치지 않는다.
// 넣은 뒤 목록 캐시가 비워지는지 확인한다. 실제 100만 건 삽입은 하지 않도록 JdbcTemplate을 mock으로 바꾼다.
@SpringBootTest
class DummyDataServiceCacheEvictionTest {

    @Autowired
    private DummyDataService dummyDataService;
    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void 더미_데이터를_넣으면_목록_캐시가_비워진다() {
        given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).willReturn(0);
        Cache cache = cacheManager.getCache(ProductCacheService.PRODUCTS_CACHE);
        String key = "더미캐시-" + UUID.randomUUID();
        cache.put(key, new ArrayList<>()); // 실제 캐시 값과 같은 타입(List.of()는 타입 정보 없이 직렬화돼 다시 읽지 못함)
        assertThat(cache.get(key)).isNotNull();

        dummyDataService.insertDummyProducts();

        assertThat(cache.get(key)).isNull();
    }
}
