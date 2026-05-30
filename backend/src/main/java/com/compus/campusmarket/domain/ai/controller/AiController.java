package com.compus.campusmarket.domain.ai.controller;

import com.compus.campusmarket.domain.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map; // ✅ Map 임포트 추가

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // 상품 설명 자동 생성 API
    @GetMapping("/generate-description")
    public ResponseEntity<Map<String, String>> generateDescription(
            @RequestParam String title,
            @RequestParam String category) {

        String aiDescription = aiService.generateProductDescription(title, category);

        // ✅ 프론트엔드가 기대하는 { "description": "생성된 내용" } 형태의 JSON 객체로 반환!
        return ResponseEntity.ok(Map.of("description", aiDescription));
    }
}