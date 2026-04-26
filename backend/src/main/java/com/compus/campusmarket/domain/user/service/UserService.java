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

    @Transactional
    public Long signUp(UserSignUpRequest request) {
        // 중복 검증 (이메일 및 학번)
        validateDuplicateUser(request.getEmail(), request.getStudentId());

        User user = User.create(
                request.getEmail(),
                request.getName(),
                request.getStudentId(),
                request.getDepartment(),
                request.getPassword() // 주의: 현재는 평문 저장, 보안상 암호화가 권장됨
        );

        return userRepository.save(user).getId();
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