package com.compus.campusmarket.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// SecurityConfig의 인증 실패 응답과 형식을 맞춰 {"message": "..."}으로 통일
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> handleConcurrencyFailure(Exception e) {
        log.warn("동시 요청 충돌: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "다른 요청과 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버에 오류가 발생했습니다."));
    }
}
