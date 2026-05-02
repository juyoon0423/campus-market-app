package com.compus.campusmarket.domain.user.entity;

import com.compus.campusmarket.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")                   // 'user'는 MySQL 예약어라 복수형 사용
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 기본 생성자, 외부 직접 호출 방지
@ToString(exclude = "items")             // 순환참조 방지
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String studentId;            // 학번

    @Column(nullable = false, length = 50)
    private String department;           // 학과

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private double trustScore = 0.0;     // 신뢰 지수 (별점), 기본값 0

    @Enumerated(EnumType.STRING)         // DB에 문자열로 저장 (ACTIVE/INACTIVE/BANNED)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // 정적 팩토리 메서드: new User() 대신 User.create()로 의미 명확하게
    public static User create(String email, String name, String studentId,
                              String department, String password) {
        User user = new User();
        user.email = email;
        user.name = name;
        user.studentId = studentId;
        user.department = department;
        user.password = password;
        return user;
    }

    // 신뢰 지수 업데이트 (비즈니스 로직은 엔티티 안에)
    public void updateTrustScore(double newScore) {
        this.trustScore = newScore;
    }
}