package com.compus.campusmarket.domain.product.dto;

import com.compus.campusmarket.domain.product.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    @NotNull(message = "변경할 상태를 지정해주세요.")
    private ProductStatus status;
    private Long buyerId;  // SOLD_OUT일 때 필요
}