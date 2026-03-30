package com.example.bankcards.dto.card;

import java.math.BigDecimal;

public record CardBalanceResponse(
        Long cardId,
        String maskedCardNumber,
        BigDecimal balance,
        String status
) {
}
