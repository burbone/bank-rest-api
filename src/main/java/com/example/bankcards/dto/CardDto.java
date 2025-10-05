package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private Long id;
    private String maskedCardNumber;
    private String cardHolder;
    private LocalDate expireDate;
    private String status;
    private BigDecimal balance;
    private Long ownerId;
    private String ownerUsername;
}
