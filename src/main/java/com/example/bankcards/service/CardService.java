package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardBalanceResponse;
import com.example.bankcards.dto.card.CardFilterRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CardService {

    CardResponse createCard(CreateCardRequest request);

    PageResponse<CardResponse> getAllCards(CardFilterRequest filter, Pageable pageable);

    CardResponse getCard(Long cardId);

    PageResponse<CardResponse> getCurrentUserCards(Long userId, CardFilterRequest filter, Pageable pageable);

    CardResponse getCurrentUserCard(Long userId, Long cardId);

    CardBalanceResponse getCurrentUserCardBalance(Long userId, Long cardId);

    CardResponse requestBlock(Long userId, Long cardId);

    CardResponse activateCard(Long cardId);

    CardResponse blockCard(Long cardId);

    void deleteCard(Long cardId);
}
