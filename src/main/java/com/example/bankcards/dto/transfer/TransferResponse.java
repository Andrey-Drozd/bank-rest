package com.example.bankcards.dto.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long fromCardId,
        String fromCardMaskedNumber,
        Long toCardId,
        String toCardMaskedNumber,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
