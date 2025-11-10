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
public class CancelUseResponse {
    
    private String cancelUsePointKey;
    private String originalUsePointKey;
    private Long canceledAmount;
    private Long totalBalance;
    private List<RestoredPointDetail> restoredPoints;
    private List<NewlyEarnedPointDetail> newlyEarnedPoints;
    private LocalDateTime canceledAt;
}
