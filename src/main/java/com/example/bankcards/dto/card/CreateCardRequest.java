package com.example.bankcards.dto.card;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCardRequest(
        @NotNull Long ownerId,
        @NotNull @Future LocalDate expirationDate,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal balance
) {
}
