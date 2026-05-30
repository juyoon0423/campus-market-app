package com.compus.campusmarket.domain.ai.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class GeminiRequest {

    private List<Content> contents;

    public GeminiRequest(String text) {
        this.contents = List.of(new Content(text));
    }

    @Getter
    public static class Content {
        private List<Part> parts;
        public Content(String text) {
            this.parts = List.of(new Part(text));
        }
    }

    @Getter
    public static class Part {
        private String text;
        public Part(String text) {
            this.text = text;
        }
    }
}