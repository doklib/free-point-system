package com.musinsa.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoredPointDetail {
    
    private String earnPointKey;
    private Long restoredAmount;
    private Boolean isExpired;
}
