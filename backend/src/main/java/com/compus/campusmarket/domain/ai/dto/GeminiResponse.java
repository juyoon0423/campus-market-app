package com.compus.campusmarket.domain.ai.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class GeminiResponse {

    private List<Candidate> candidates;

    @Getter
    public static class Candidate {
        private Content content;
    }

    @Getter
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    public static class Part {
        private String text;
    }

    // 텍스트만 쏙 뽑아내는 편의 메서드
    public String getExtractedText() {
        if (candidates != null && !candidates.isEmpty() &&
                candidates.get(0).getContent() != null &&
                candidates.get(0).getContent().getParts() != null &&
                !candidates.get(0).getContent().getParts().isEmpty()) {

            return candidates.get(0).getContent().getParts().get(0).getText();
        }
        return "";
    }
}