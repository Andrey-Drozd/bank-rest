package com.example.bankcards.dto.card;

import com.example.bankcards.entity.enums.CardStatus;

public record CardFilterRequest(
        Long ownerId,
        String query,
        CardStatus status,
        Boolean blockRequested
) {
}
