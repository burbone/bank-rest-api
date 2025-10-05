package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private String fromCardMasked;
    private String toCardMasked;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String status;
    private String description;
}