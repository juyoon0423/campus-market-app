package com.compus.campusmarket.domain.dummy.controller;

import com.compus.campusmarket.domain.dummy.service.DummyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dummy")
@RequiredArgsConstructor
public class DummyDataController {

    private final DummyDataService dummyDataService;

    @PostMapping("/products")
    public ResponseEntity<String> createDummyProducts() {
        dummyDataService.insertDummyProducts();
        return ResponseEntity.ok("100만 건 더미 데이터 생성 시작 (서버 로그를 확인하세요)");
    }
}