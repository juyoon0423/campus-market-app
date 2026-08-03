package com.compus.campusmarket.global.util;

import com.compus.campusmarket.global.config.auth.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final CustomUserDetailsService userDetailsService;
    private final SecretKey key;
    private final long expirationTime = 3600000; // 1시간

    public JwtTokenProvider(CustomUserDetailsService userDetailsService,
                             @Value("${jwt.secret}") String secret) {
        this.userDetailsService = userDetailsService;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationTime))
                .signWith(key)
                .compact();
    }

    // [중요] 실무형: Authentication 객체 직접 생성
    public Authentication getAuthentication(String token) {
        Long userId = this.getUserIdFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(userId));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}