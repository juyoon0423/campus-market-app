package com.compus.campusmarket.global.config;

import com.compus.campusmarket.global.util.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// STOMP CONNECT 프레임에 대한 JWT 검증(StompAuthChannelInterceptor)이 실제로 동작하는지 확인한다.
// 이 인터셉터가 추가되기 전에는 /ws-stomp가 사실상 무인증이라 누구든 접속해 임의의 senderId로
// 메시지를 보낼 수 있었다.
@SpringBootTest
class StompAuthChannelInterceptorTest {

    @Autowired
    private StompAuthChannelInterceptor interceptor;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 유효한_토큰이_있으면_CONNECT에_사용자가_설정된다() {
        String token = jwtTokenProvider.createToken(42L);
        Message<byte[]> connectMessage = connectMessageWithAuthorization("Bearer " + token);

        Message<?> result = interceptor.preSend(connectMessage, null);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo("42");
    }

    @Test
    void 토큰이_없으면_CONNECT가_거부된다() {
        Message<byte[]> connectMessage = connectMessageWithAuthorization(null);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 위조된_토큰이면_CONNECT가_거부된다() {
        Message<byte[]> connectMessage = connectMessageWithAuthorization("Bearer not-a-real-jwt");

        assertThatThrownBy(() -> interceptor.preSend(connectMessage, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Message<byte[]> connectMessageWithAuthorization(String authorizationHeaderValue) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeaderValue != null) {
            accessor.setNativeHeader("Authorization", authorizationHeaderValue);
        }
        accessor.setLeaveMutable(true);
        return org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
