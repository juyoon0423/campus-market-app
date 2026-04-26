package com.compus.campusmarket.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class UserSignUpRequest {
    private String email;
    private String name;
    private String studentId;
    private String department;
    private String password;
}