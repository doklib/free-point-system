package com.musinsa.point.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelEarnRequest {
    
    @NotBlank(message = "포인트 키는 필수입니다")
    private String pointKey;
    
    private String reason;
}
