package com.compus.campusmarket.domain.user.controller;

import com.compus.campusmarket.domain.user.dto.*;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.service.UserService;
import com.compus.campusmarket.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // 필수 import
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserSignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok("회원가입 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserLoginRequest loginRequest) {
        User loginUser = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

        // 토큰 생성
        String token = jwtTokenProvider.createToken(loginUser.getId());

        Map<String, String> response = new HashMap<>();
        response.put("token", token); // 프론트엔드에서 이 토큰을 저장해야 함
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        // [수정] 세션 어노테이션을 버리고 Authentication을 사용
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = (Long) authentication.getPrincipal();
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    // JWT 방식에서는 로그아웃을 프론트엔드에서 토큰을 삭제하는 것으로 처리하므로
    // 서버측 로그아웃 엔드포인트는 보통 필요 없거나 블랙리스트 처리를 합니다.
}