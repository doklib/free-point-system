package com.musinsa.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotBlank(message = "주문 번호는 필수입니다")
    private String orderNumber;
    
    @NotNull(message = "사용 금액은 필수입니다")
    @Min(value = 1, message = "사용 금액은 1포인트 이상이어야 합니다")
    private Long amount;
}
