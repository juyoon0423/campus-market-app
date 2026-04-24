// global/config/LoginCheckInterceptor.java
package com.compus.campusmarket.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("loginUser") == null) {
            // 로그인이 안 된 경우 401 에러를 반환하거나 로그인 페이지로 리다이렉트
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthenticated: Please login first.");
            return false; // 다음 단계(컨트롤러)로 넘어가지 않음
        }

        return true; // 로그인 된 경우 컨트롤러 진입 허용
    }
}