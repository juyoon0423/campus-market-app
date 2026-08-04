package com.compus.campusmarket.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductCreateRequest { // 혹은 ProductUpdateRequest
    @NotBlank(message = "상품명을 입력해주세요.")
    private String title;

    @NotBlank(message = "상품 설명을 입력해주세요.")
    private String description;

    @NotNull(message = "가격을 입력해주세요.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private Long price;

    @NotBlank(message = "카테고리를 선택해주세요.") // 이 필드가 반드시 있어야 합니다.
    private String category;
}