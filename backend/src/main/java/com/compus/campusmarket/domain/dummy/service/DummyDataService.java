package com.compus.campusmarket.domain.dummy.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void insertDummyProducts() {
        // 이미 데이터가 많다면 실행하지 않음
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Integer.class);
        if (count != null && count > 100000) {
            log.info("이미 충분한 더미 데이터가 존재합니다.");
            return;
        }

        String sql = "INSERT INTO product " +
                "(title, description, price, category, status, view_count, like_count, user_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        int batchSize = 10000; // 1만 건씩 끊어서 삽입
        int totalRecords = 1000000; // 총 100만 건

        List<Object[]> batchArgs = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalRecords; i++) {
            // "아이패드 1", "아이패드 2" ... 중간중간 다른 키워드 섞기
            String title = (i % 10 == 0) ? "맥북 프로 " + i : "아이패드 " + i;
            String category = (i % 10 == 0) ? "노트북" : "전자기기";

            Object[] values = new Object[]{
                    title,
                    "더미 상품 설명입니다. 훌륭한 상태입니다.", // description
                    (long) (Math.random() * 100000) + 10000, // price (1만~11만)
                    category, // category
                    "SELLING", // status (ProductStatus.SELLING의 문자열)
                    0, // view_count
                    0, // like_count
                    1L // user_id (🚨주의: DB에 id가 1인 유저가 반드시 존재해야 함!)
            };
            batchArgs.add(values);

            // batchSize만큼 모이면 DB에 한 번에 쏘고 리스트 비우기
            if (i % batchSize == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                log.info("{} 만 건 삽입 완료...", i / 10000);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("100만 건 더미 데이터 삽입 완료! 소요 시간: {} ms", (endTime - startTime));
    }
}
