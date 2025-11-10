package com.musinsa.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelEarnResponse {
    
    private String cancelPointKey;
    private String originalPointKey;
    private Long canceledAmount;
    private Long totalBalance;
    private LocalDateTime canceledAt;
}
