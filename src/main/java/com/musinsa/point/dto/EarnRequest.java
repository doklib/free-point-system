package com.musinsa.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
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
public class EarnRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotNull(message = "적립 금액은 필수입니다")
    @Min(value = 1, message = "적립 금액은 1포인트 이상이어야 합니다")
    @Max(value = 100000, message = "적립 금액은 100,000포인트 이하여야 합니다")
    private Long amount;
    
    @NotNull(message = "수기 지급 여부는 필수입니다")
    private Boolean isManualGrant;
    
    @Min(value = 1, message = "만료일은 최소 1일 이상이어야 합니다")
    @Max(value = 1825, message = "만료일은 최대 1825일(5년) 이하여야 합니다")
    private Integer expirationDays;
    
    private String description;
}
