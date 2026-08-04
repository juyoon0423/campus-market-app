package com.compus.campusmarket.global.config;

import com.compus.campusmarket.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// STOMP CONNECT 프레임에 JWT가 없거나 유효하지 않으면 연결을 거부한다.
// HTTP 요청과 달리 WebSocket 세션은 SecurityConfig의 JwtAuthenticationFilter를 거치지 않기 때문에
// 여기서 별도로 검증하지 않으면 누구나 무인증으로 채팅 메시지를 송수신할 수 있었다.
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    // MessageHeaderAccessor.getAccessor(message, Class)는 Spring 5.5부터 deprecated지만,
    // StompHeaderAccessor.wrap(message)로 바꿨다가 실제로 채팅이 깨지는 걸 실기동 테스트에서 확인했다:
    // StompSubProtocolHandler는 CONNECT 메시지를 만들 때 그 accessor 인스턴스에
    // setUserChangeCallback(...)을 걸어두고, 이후 프레임에서 Principal을 이 콜백이 기록해둔 값으로
    // 채운다. wrap()은 매번 새 accessor 객체를 만들어 그 콜백 연결이 끊기므로 setUser()를 호출해도
    // 아무 효과가 없어(SEND 시점에 Principal이 null) 메시지 전송이 NPE로 죽었다. getAccessor()는
    // 메시지에 실려온 "그" accessor 인스턴스(콜백 포함)를 그대로 돌려주므로 이 콜백이 정상 동작한다.
    @SuppressWarnings("deprecation")
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveToken(accessor);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 인증 정보로 웹소켓에 연결할 수 없습니다.");
        }
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        accessor.setUser(() -> String.valueOf(userId));

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
