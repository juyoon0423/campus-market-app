package com.compus.campusmarket.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.boot.convert.ApplicationConversionService;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 쿼리 파라미터로 들어오는 문자열을 Enum으로 변환할 때
     * 대소문자를 무시하고 매핑해주는 컨버터를 등록합니다.
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        ApplicationConversionService.configure(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 이미지 외부 노출 설정
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir);
    }
}