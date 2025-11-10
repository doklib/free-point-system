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
public class NewlyEarnedPointDetail {
    
    private String pointKey;
    private Long amount;
    private LocalDateTime expirationDate;
}
