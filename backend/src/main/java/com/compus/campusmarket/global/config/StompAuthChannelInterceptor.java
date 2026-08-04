package com.compus.campusmarket.global.config;

import com.compus.campusmarket.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// STOMP CONNECT 프레임에 JWT가 없거나 유효하지 않으면 연결을 거부한다.
// HTTP 요청과 달리 WebSocket 세션은 SecurityConfig의 JwtAuthenticationFilter를 거치지 않기 때문에
// 여기서 별도로 검증하지 않으면 누구나 무인증으로 채팅 메시지를 송수신할 수 있었다.
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveToken(accessor);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 인증 정보로 웹소켓에 연결할 수 없습니다.");
        }
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        accessor.setUser(() -> String.valueOf(userId));

        // wrap()으로 만든 접근자는 헤더의 복사본을 다루므로, 변경 사항을 실제로 반영하려면
        // 이 헤더로 메시지를 다시 만들어 반환해야 한다(그냥 message를 반환하면 setUser가 무시됨).
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
