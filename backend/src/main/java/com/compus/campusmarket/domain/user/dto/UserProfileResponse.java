package com.compus.campusmarket.domain.user.dto;

import com.compus.campusmarket.domain.user.entity.User;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class UserProfileResponse {
    private String name;
    private String studentId;
    private String department;
    private double trustScore;
    private LocalDateTime createdAt;

    public UserProfileResponse(User user) {
        this.name = user.getName();
        this.studentId = user.getStudentId();
        this.department = user.getDepartment();
        this.trustScore = user.getTrustScore();
        this.createdAt = user.getCreatedAt();
    }
}