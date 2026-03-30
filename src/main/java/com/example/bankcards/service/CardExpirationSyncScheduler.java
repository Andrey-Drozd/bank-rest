package com.example.bankcards.service;

import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpirationSyncScheduler {

    private final CardRepository cardRepository;

    @Scheduled(cron = "${app.cards.expiration-sync-cron:0 0 * * * *}")
    @Transactional
    public void markExpiredCards() {
        int updated = cardRepository.markExpiredCards(LocalDate.now(), CardStatus.EXPIRED);

        if (updated > 0) {
            log.info("Marked {} cards as expired", updated);
        }
    }
}
