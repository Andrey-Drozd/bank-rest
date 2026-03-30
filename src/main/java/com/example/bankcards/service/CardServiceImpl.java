package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardBalanceResponse;
import com.example.bankcards.dto.card.CardFilterRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberCodec;
import com.example.bankcards.util.CardViewHelper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private static final int CARD_NUMBER_GENERATION_ATTEMPTS = 100;

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardNumberCodec cardNumberCodec;
    private final CardViewHelper cardViewHelper;

    @Override
    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User owner = userRepository.findByIdAndDeletedFalse(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Card owner not found"));

        if (!Boolean.TRUE.equals(owner.getEnabled())) {
            throw new ConflictException("Card owner is disabled");
        }

        String cardNumber = generateUniqueCardNumber();

        Card card = new Card();
        card.setOwner(owner);
        card.setCardNumberEncrypted(cardNumberCodec.encrypt(cardNumber));
        card.setCardNumberHash(cardNumberCodec.hash(cardNumber));
        card.setCardNumberMasked(cardNumberCodec.mask(cardNumber));
        card.setExpirationDate(request.expirationDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(normalizeAmount(request.balance()));
        card.setBlockRequested(false);

        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getAllCards(CardFilterRequest filter, Pageable pageable) {
        Page<CardResponse> page = cardRepository.findAll(buildSpecification(filter), pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCard(Long cardId) {
        return toResponse(getCardById(cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getCurrentUserCards(Long userId, CardFilterRequest filter, Pageable pageable) {
        CardFilterRequest scopedFilter = new CardFilterRequest(userId, filter.query(), filter.status(), filter.blockRequested());
        Page<CardResponse> page = cardRepository.findAll(buildSpecification(scopedFilter), pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCurrentUserCard(Long userId, Long cardId) {
        return toResponse(getOwnedCard(userId, cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public CardBalanceResponse getCurrentUserCardBalance(Long userId, Long cardId) {
        Card card = getOwnedCard(userId, cardId);
        return new CardBalanceResponse(
                card.getId(),
                cardViewHelper.maskCardNumber(card),
                card.getBalance(),
                cardViewHelper.resolveStatus(card).name()
        );
    }

    @Override
    @Transactional
    public CardResponse requestBlock(Long userId, Long cardId) {
        Card card = getOwnedCard(userId, cardId);
        assertCardNotExpired(card);

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ConflictException("Card is already blocked");
        }

        if (Boolean.TRUE.equals(card.getBlockRequested())) {
            throw new ConflictException("Block request is already pending");
        }

        card.setBlockRequested(true);
        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    public CardResponse activateCard(Long cardId) {
        Card card = getCardById(cardId);
        assertCardNotExpired(card);

        if (card.getStatus() == CardStatus.ACTIVE && !Boolean.TRUE.equals(card.getBlockRequested())) {
            throw new ConflictException("Card is already active");
        }

        card.setStatus(CardStatus.ACTIVE);
        card.setBlockRequested(false);

        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = getCardById(cardId);
        assertCardNotExpired(card);

        if (card.getStatus() == CardStatus.BLOCKED && !Boolean.TRUE.equals(card.getBlockRequested())) {
            throw new ConflictException("Card is already blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        card.setBlockRequested(false);

        return toResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = getCardById(cardId);

        try {
            cardRepository.delete(card);
            cardRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Card cannot be deleted because it has related operations");
        }
    }

    private Specification<Card> buildSpecification(CardFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.ownerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("owner").get("id"), filter.ownerId()));
            }

            if (filter.query() != null && !filter.query().isBlank()) {
                String pattern = "%" + filter.query().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("cardNumberMasked")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("owner").get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("owner").get("email")), pattern)
                ));
            }

            if (filter.status() != null) {
                LocalDate today = LocalDate.now();
                if (filter.status() == CardStatus.EXPIRED) {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("status"), CardStatus.EXPIRED),
                            criteriaBuilder.lessThan(root.get("expirationDate"), today)
                    ));
                } else {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("expirationDate"), today));
                }
            }

            if (filter.blockRequested() != null) {
                predicates.add(criteriaBuilder.equal(root.get("blockRequested"), filter.blockRequested()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Card getOwnedCard(Long userId, Long cardId) {
        return cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private void assertCardNotExpired(Card card) {
        if (cardViewHelper.isExpired(card)) {
            throw new ConflictException("Expired card cannot be changed");
        }
    }

    private CardResponse toResponse(Card card) {
        CardStatus status = cardViewHelper.resolveStatus(card);
        return new CardResponse(
                card.getId(),
                cardViewHelper.maskCardNumber(card),
                card.getOwner().getId(),
                card.getOwner().getUsername(),
                card.getOwner().getEmail(),
                card.getExpirationDate(),
                status.name(),
                card.getBalance(),
                cardViewHelper.resolveBlockRequested(card),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    private String generateUniqueCardNumber() {
        for (int attempt = 0; attempt < CARD_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            String cardNumber = cardNumberCodec.generateCardNumber();
            String cardNumberHash = cardNumberCodec.hash(cardNumber);

            if (!cardRepository.existsByCardNumberHash(cardNumberHash)) {
                return cardNumber;
            }
        }

        throw new IllegalStateException("Failed to generate unique card number");
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
