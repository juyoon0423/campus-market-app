// domain/user/controller/UserController.java
package com.compus.campusmarket.domain.user.controller;

import com.compus.campusmarket.domain.user.dto.UserLoginRequest;
import com.compus.campusmarket.domain.user.dto.UserSignUpRequest;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserSignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        User loginUser = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

        // 세션 생성
        HttpSession session = request.getSession();
        session.setAttribute("loginUser", loginUser.getId()); // 세션에 유저 고유 ID 저장

        return ResponseEntity.ok("로그인 성공: " + loginUser.getName() + "님 환영합니다.");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 무효화
        }
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}