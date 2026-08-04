package com.compus.campusmarket.domain.user.service;

import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 비밀번호 평문 저장/비교(SECURITY_REFACTOR_TODO.md 1번)를 BCrypt로 바꾼 뒤,
// 저장된 값이 평문이 아니고 login()이 해시 비교로 정상 동작하는지 검증한다.
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @AfterEach
    void tearDown() {
        if (user != null) userRepository.deleteById(user.getId());
    }

    @Test
    void 비밀번호는_해시로_저장되고_로그인은_원본_비밀번호로_성공한다() {
        String rawPassword = "password123!";
        user = userRepository.save(User.create(
                "user-service-test-1@sj.sangji.ac.kr", "테스트", "20990001", "컴퓨터공학과",
                passwordEncoder.encode(rawPassword)));

        assertThat(user.getPassword()).isNotEqualTo(rawPassword);

        User loggedIn = userService.login("user-service-test-1@sj.sangji.ac.kr", rawPassword);

        assertThat(loggedIn.getId()).isEqualTo(user.getId());
    }

    @Test
    void 비밀번호가_틀리면_로그인에_실패한다() {
        user = userRepository.save(User.create(
                "user-service-test-2@sj.sangji.ac.kr", "테스트", "20990002", "컴퓨터공학과",
                passwordEncoder.encode("password123!")));

        assertThatThrownBy(() -> userService.login("user-service-test-2@sj.sangji.ac.kr", "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
