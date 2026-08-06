package com.compus.campusmarket.domain.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailService emailService = new EmailService(mailSender);

    @Test
    void 발급된_코드로_인증하면_성공한다() {
        emailService.sendVerificationCode("test1@sj.sangji.ac.kr");

        // 실제로 발송된 코드는 모킹된 mailSender로는 알 수 없으므로, 테스트 헬퍼로 코드를 직접 심는다.
        emailService.putVerificationCodeForTest("test2@sj.sangji.ac.kr", "123456", Instant.now().plusSeconds(60));

        boolean result = emailService.verifyCode("test2@sj.sangji.ac.kr", "123456");

        assertThat(result).isTrue();
        assertThat(emailService.isVerified("test2@sj.sangji.ac.kr")).isTrue();
    }

    @Test
    void 만료된_코드는_일치해도_인증에_실패한다() {
        emailService.putVerificationCodeForTest("expired@sj.sangji.ac.kr", "123456", Instant.now().minusSeconds(1));

        boolean result = emailService.verifyCode("expired@sj.sangji.ac.kr", "123456");

        assertThat(result).isFalse();
        assertThat(emailService.isVerified("expired@sj.sangji.ac.kr")).isFalse();
    }

    @Test
    void 틀린_코드는_인증에_실패한다() {
        emailService.putVerificationCodeForTest("wrong@sj.sangji.ac.kr", "123456", Instant.now().plusSeconds(60));

        boolean result = emailService.verifyCode("wrong@sj.sangji.ac.kr", "000000");

        assertThat(result).isFalse();
        assertThat(emailService.isVerified("wrong@sj.sangji.ac.kr")).isFalse();
    }
}
