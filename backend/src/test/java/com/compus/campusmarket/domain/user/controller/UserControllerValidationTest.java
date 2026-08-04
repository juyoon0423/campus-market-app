package com.compus.campusmarket.domain.user.controller;

import com.compus.campusmarket.domain.user.dto.UserSignUpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DTO에 붙인 Bean Validation 애너테이션이 실제로 @Valid -> GlobalExceptionHandler까지
// 이어져 400을 반환하는지 엔드투엔드로 확인한다(어노테이션만 붙이고 @Valid를 빠뜨리는 실수를 잡기 위함).
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 회원가입_비밀번호가_8자_미만이면_400을_반환한다() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest();
        request.setEmail("validation-test-1@sj.sangji.ac.kr");
        request.setName("테스트");
        request.setStudentId("20999901");
        request.setDepartment("컴퓨터공학과");
        request.setPassword("1234567"); // 7자 — 최소 길이 미달

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 회원가입_이메일_형식이_아니면_400을_반환한다() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest();
        request.setEmail("not-an-email");
        request.setName("테스트");
        request.setStudentId("20999902");
        request.setDepartment("컴퓨터공학과");
        request.setPassword("password123");

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
