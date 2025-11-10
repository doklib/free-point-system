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
public class CancelUseRequest {
    
    @NotBlank(message = "사용 포인트 키는 필수입니다")
    private String usePointKey;
    
    @NotNull(message = "취소 금액은 필수입니다")
    @Min(value = 1, message = "취소 금액은 1포인트 이상이어야 합니다")
    private Long amount;
    
    private String reason;
}
