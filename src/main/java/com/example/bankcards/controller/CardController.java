package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardBalanceResponse;
import com.example.bankcards.dto.card.CardFilterRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse createCard(@Valid @RequestBody CreateCardRequest request) {
        return cardService.createCard(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CardResponse> getAllCards(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) Boolean blockRequested,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size
    ) {
        CardFilterRequest filter = new CardFilterRequest(ownerId, query, status, blockRequested);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return cardService.getAllCards(filter, pageable);
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse getCard(@PathVariable Long cardId) {
        return cardService.getCard(cardId);
    }

    @PatchMapping("/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse activateCard(@PathVariable Long cardId) {
        return cardService.activateCard(cardId);
    }

    @PatchMapping("/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse blockCard(@PathVariable Long cardId) {
        return cardService.blockCard(cardId);
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PageResponse<CardResponse> getMyCards(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) Boolean blockRequested,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size
    ) {
        CardFilterRequest filter = new CardFilterRequest(null, query, status, blockRequested);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return cardService.getCurrentUserCards(principal.id(), filter, pageable);
    }

    @GetMapping("/me/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CardResponse getMyCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId
    ) {
        return cardService.getCurrentUserCard(principal.id(), cardId);
    }

    @GetMapping("/me/{cardId}/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CardBalanceResponse getMyCardBalance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId
    ) {
        return cardService.getCurrentUserCardBalance(principal.id(), cardId);
    }

    @PostMapping("/me/{cardId}/block-request")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CardResponse requestBlock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cardId
    ) {
        return cardService.requestBlock(principal.id(), cardId);
    }
}
