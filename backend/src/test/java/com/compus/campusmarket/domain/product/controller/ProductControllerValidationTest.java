package com.compus.campusmarket.domain.product.controller;

import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import com.compus.campusmarket.global.util.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 상품 등록은 @RequestPart("data")로 JSON을 받기 때문에 @RequestBody와는 바인딩 경로가 달라
// @Valid가 실제로 적용되는지 별도로 확인한다.
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User seller;

    @AfterEach
    void tearDown() {
        if (seller != null) userRepository.deleteById(seller.getId());
    }

    @Test
    void 상품_등록시_가격이_음수면_400을_반환한다() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        seller = userRepository.save(User.create(
                "product-valid-" + suffix + "@sj.sangji.ac.kr", "판매자", "S-" + suffix, "컴퓨터공학과", "password12"));
        String token = jwtTokenProvider.createToken(seller.getId());

        String invalidJson = "{\"title\":\"상품\",\"description\":\"설명\",\"price\":-1000,\"category\":\"전자기기\"}";
        MockMultipartFile data = new MockMultipartFile(
                "data", "", "application/json", invalidJson.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/products")
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
