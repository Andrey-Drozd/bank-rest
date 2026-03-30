package com.example.bankcards.util;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CardViewHelper {

    private final CardNumberCodec cardNumberCodec;

    public String maskCardNumber(Card card) {
        if (card.getCardNumberMasked() != null && !card.getCardNumberMasked().isBlank()) {
            return card.getCardNumberMasked();
        }

        return cardNumberCodec.mask(cardNumberCodec.decrypt(card.getCardNumberEncrypted()));
    }

    public CardStatus resolveStatus(Card card) {
        if (isExpired(card)) {
            return CardStatus.EXPIRED;
        }

        return card.getStatus();
    }

    public boolean resolveBlockRequested(Card card) {
        return resolveStatus(card) != CardStatus.EXPIRED && Boolean.TRUE.equals(card.getBlockRequested());
    }

    public boolean isExpired(Card card) {
        return card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now());
    }
}
