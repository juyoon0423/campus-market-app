package com.compus.campusmarket.domain.ai.service;

import com.compus.campusmarket.domain.ai.dto.GeminiRequest;
import com.compus.campusmarket.domain.ai.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    // Spring Boot 3.2+ 의 모던한 HTTP 클라이언트
    private final RestClient restClient = RestClient.create();

    // 1. 상품 설명 자동 생성 기능
    public String generateProductDescription(String title, String category) {
        String prompt = String.format(
                "너는 중고거래 앱의 친절한 판매자야. " +
                        "다음 정보를 바탕으로 구매자의 시선을 끄는 매력적인 중고거래 판매글을 300자 이내로 작성해줘. " +
                        "이모지도 적절히 섞어서 써줘. " +
                        "상품명: %s, 카테고리: %s", title, category);

        return callGeminiApi(prompt);
    }

    // 2. 부적절한 매물 필터링 기능
    public boolean isAppropriateProduct(String description) {
        String prompt = String.format(
                "다음 중고거래 게시글 내용에 욕설, 마약, 무기, 불법적인 내용이 포함되어 있는지 검사해줘. " +
                        "불법적이거나 부적절하다면 'false', 정상적인 거래 글이라면 'true'라고 오직 단어 하나만 대답해. " +
                        "내용: %s", description);

        String result = callGeminiApi(prompt).trim().toLowerCase();

        // 제미나이가 'true'라고 대답하면 적절한 글, 아니면 부적절한 글로 판단
        return result.contains("true");
    }

    // Gemini API 통신 공통 메서드
    private String callGeminiApi(String prompt) {
        try {
            GeminiRequest request = new GeminiRequest(prompt);

            GeminiResponse response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            return response != null ? response.getExtractedText() : "AI 응답을 불러오지 못했습니다.";

        } catch (Exception e) {
            log.error("Gemini API 호출 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("AI 서비스 연결에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}