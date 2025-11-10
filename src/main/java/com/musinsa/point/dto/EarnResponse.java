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
public class EarnResponse {
    
    private String pointKey;
    private String userId;
    private Long amount;
    private Long availableBalance;
    private Long totalBalance;
    private LocalDateTime expirationDate;
    private Boolean isManualGrant;
    private LocalDateTime createdAt;
}
