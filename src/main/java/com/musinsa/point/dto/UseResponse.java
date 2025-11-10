package com.musinsa.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseResponse {
    
    private String usePointKey;
    private String userId;
    private String orderNumber;
    private Long usedAmount;
    private Long remainingBalance;
    private List<UsedFromDetail> usedFrom;
    private LocalDateTime usedAt;
}
