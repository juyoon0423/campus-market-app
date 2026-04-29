package com.compus.campusmarket.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 백엔드 WebSocketConfig.java 예시
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // 또는 특정 도메인
                .withSockJS(); // 프론트에서 SockJS를 쓰면 필수!
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 구독(수신)하는 경로 접두사
        registry.enableSimpleBroker("/sub");
        // 메시지를 발행(송신)하는 경로 접두사
        registry.setApplicationDestinationPrefixes("/pub");
    }
}