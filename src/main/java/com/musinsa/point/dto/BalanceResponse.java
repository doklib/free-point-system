package com.musinsa.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    
    private String userId;
    private Long totalBalance;
    private List<AvailablePointDetail> availablePoints;
}
