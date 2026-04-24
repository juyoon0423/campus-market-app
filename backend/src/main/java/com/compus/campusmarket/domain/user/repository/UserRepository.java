// domain/user/repository/UserRepository.java
package com.compus.campusmarket.domain.user.repository;

import com.compus.campusmarket.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByStudentId(String studentId);
}