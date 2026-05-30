package com.compus.campusmarket.domain.user.service;

import com.compus.campusmarket.domain.user.dto.UserProfileResponse;
import com.compus.campusmarket.domain.user.dto.UserSignUpRequest;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService; // 👈 상단 의존성 주입에 추가

    @Transactional
    public Long signUp(UserSignUpRequest request) {
        // 🚨 1. 학교 이메일 도메인인지 한 번 더 검증
        if (!request.getEmail().endsWith("@sj.sangji.ac.kr")) {
            throw new IllegalArgumentException("상지대학교 학생만 가입할 수 있습니다.");
        }

        // 🚨 2. 이메일 인증을 완료했는지 확인
        if (!emailService.isVerified(request.getEmail())) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 3. 중복 검증 (이메일 및 학번)
        validateDuplicateUser(request.getEmail(), request.getStudentId());

        User user = User.create(
                request.getEmail(),
                request.getName(),
                request.getStudentId(),
                request.getDepartment(),
                request.getPassword()
        );

        Long savedUserId = userRepository.save(user).getId();

        // 4. 가입 완료 후 인증 상태 삭제 (메모리 정리)
        emailService.removeVerificationStatus(request.getEmail());

        return savedUserId;
    }

    public User login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password))
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));
    }

    private void validateDuplicateUser(String email, String studentId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
        if (userRepository.findByStudentId(studentId).isPresent()) {
            throw new IllegalStateException("이미 등록된 학번입니다.");
        }
    }
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return new UserProfileResponse(user);
    }

}