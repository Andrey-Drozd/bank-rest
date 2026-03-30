package com.example.bankcards.dto.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String maskedCardNumber,
        Long ownerId,
        String ownerUsername,
        String ownerEmail,
        LocalDate expirationDate,
        String status,
        BigDecimal balance,
        Boolean blockRequested,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
