package com.compus.campusmarket.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // 코드가 만료 없이 무한정 유효하면(예전 구현) 브루트포스 공격에 취약해진다.
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final JavaMailSender mailSender;

    // 이메일 - 인증코드 저장소 (실무에서는 Redis 사용 권장)
    private final Map<String, VerificationEntry> verificationCodes = new ConcurrentHashMap<>();
    // 인증 완료된 이메일 저장소
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    // 1. 인증 코드 발송
    public void sendVerificationCode(String email) {
        // 도메인 검증
        if (!email.endsWith("@sj.sangji.ac.kr")) {
            throw new IllegalArgumentException("상지대학교 이메일(@sj.sangji.ac.kr)만 가입 가능합니다.");
        }

        String code = generateRandomCode();
        verificationCodes.put(email, new VerificationEntry(code, Instant.now().plus(CODE_TTL))); // 발급된 코드 저장

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[캠퍼스 마켓] 회원가입 이메일 인증 번호");
        message.setText("안녕하세요! 캠퍼스 마켓입니다.\n인증 번호는 [" + code + "] 입니다.\n회원가입 창에 입력해주세요.");

        mailSender.send(message);
        log.info("인증 이메일 발송 완료: {}, 코드: {}", email, code);
    }

    // 2. 인증 코드 검증
    public boolean verifyCode(String email, String code) {
        VerificationEntry entry = verificationCodes.get(email);

        if (entry == null || entry.isExpired()) {
            verificationCodes.remove(email); // 만료된 코드는 더 이상 유효하지 않으니 정리
            return false;
        }

        if (!entry.code().equals(code)) {
            return false;
        }

        verificationCodes.remove(email); // 인증 성공 시 코드 삭제
        verifiedEmails.put(email, true); // 인증 완료 상태로 변경
        return true;
    }

    // 테스트에서 만료된 코드를 재현하기 위한 헬퍼(패키지 전용, 운영 코드에서는 쓰지 않음)
    void putVerificationCodeForTest(String email, String code, Instant expiresAt) {
        verificationCodes.put(email, new VerificationEntry(code, expiresAt));
    }

    private record VerificationEntry(String code, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    // 3. 회원가입 시 최종 인증 여부 확인
    public boolean isVerified(String email) {
        return verifiedEmails.getOrDefault(email, false);
    }

    // 4. 회원가입 완료 후 상태 정리
    public void removeVerificationStatus(String email) {
        verifiedEmails.remove(email);
    }

    // 6자리 난수 생성
    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}