package com.compus.campusmarket.domain.user.controller;

import com.compus.campusmarket.domain.user.dto.*;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.service.UserService;
import com.compus.campusmarket.global.config.auth.CustomUserDetails;
import com.compus.campusmarket.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // SecurityConfig에서 인증되지 않은 사용자는 401을 반환하므로
        // 여기까지 들어왔다면 userDetails는 null이 아닙니다.
        UserProfileResponse profile = userService.getUserProfile(userDetails.getUserId());
        return ResponseEntity.ok(profile);
    }
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        // UserService에 이미 구현된 getUserProfile을 호출합니다.
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
}