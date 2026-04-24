// global/config/WebMvcConfig.java
package com.compus.campusmarket.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .order(1) // 우선순위
                .addPathPatterns("/**") // 모든 경로에 적용하되
                .excludePathPatterns( // 아래 경로는 로그인 없이 접근 허용
                        "/api/users/signup",
                        "/api/users/login",
                        "/api/users/logout",
                        "/error",
                        "/favicon.ico"
                );
    }
}