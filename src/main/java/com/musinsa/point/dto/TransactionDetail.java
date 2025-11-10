package com.musinsa.point.dto;

import com.musinsa.point.domain.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetail {
    
    private String pointKey;
    private TransactionType type;
    private Long amount;
    private Long balance;
    private String orderNumber;
    private String description;
    private LocalDateTime createdAt;
}
