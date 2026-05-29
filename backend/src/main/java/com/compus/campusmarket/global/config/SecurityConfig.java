package com.compus.campusmarket.global.config;

import com.compus.campusmarket.global.config.auth.CustomOAuth2UserService;
import com.compus.campusmarket.global.config.auth.OAuth2SuccessHandler;
import com.compus.campusmarket.global.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // [수정] 필터 클래스 임포트
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 및 세션 설정 (JWT 방식이므로 Stateless)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 요청 권한 설정 (핵심 수정 부분)
                .authorizeHttpRequests(auth -> auth
                        // 누구나 접근 가능한 경로 (메서드 상관없음)
                        .requestMatchers(
                                "/api/users/signup",
                                "/api/users/login",
                                "/images/**",
                                "/favicon.ico",
                                "/error",
                                "/ws-stomp/**"
                        ).permitAll()

                        // [핵심] 상품 관련 API는 'GET(조회)' 요청만 누구나 접근 가능!
                        .requestMatchers(HttpMethod.GET,
                                "/api/products",
                                "/api/products/search",
                                "/api/products/{productId}"
                        ).permitAll()

                        // 그 외 모든 요청(상품 등록POST, 수정PATCH, 삭제DELETE, 유저조회 등)은 인증 필요
                        .anyRequest().authenticated()
                )

                // 4. 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"인증이 필요합니다. 로그인 후 다시 시도해주세요.\"}");
                        })
                )

                // 5. JWT 필터 및 OAuth2 로그인 설정
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}