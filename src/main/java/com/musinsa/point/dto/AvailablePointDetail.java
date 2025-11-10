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
public class AvailablePointDetail {
    
    private String pointKey;
    private Long amount;
    private Long availableBalance;
    private Boolean isManualGrant;
    private LocalDateTime expirationDate;
}
