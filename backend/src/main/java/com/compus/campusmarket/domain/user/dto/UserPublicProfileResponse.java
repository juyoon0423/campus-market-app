package com.compus.campusmarket.domain.user.dto;

import com.compus.campusmarket.domain.user.entity.User;
import lombok.Getter;
import java.time.LocalDateTime;

/** 본인이 아닌 다른 사용자(판매자 등)에게 공개해도 되는 프로필. 학번(studentId)처럼
 * 개인식별정보에 가까운 필드는 제외한다. */
@Getter
public class UserPublicProfileResponse {
    private String name;
    private String department;
    private double trustScore;
    private LocalDateTime createdAt;

    public UserPublicProfileResponse(User user) {
        this.name = user.getName();
        this.department = user.getDepartment();
        this.trustScore = user.getTrustScore();
        this.createdAt = user.getCreatedAt();
    }
}
